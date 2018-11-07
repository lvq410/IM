package com.lvt4j.im.common;

import static java.lang.Thread.NORM_PRIORITY;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import lombok.SneakyThrows;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;


/**
 * 各种工具方法
 * @author LV
 */
public class Utils {
    
    /**
     * 计算从开始日期(含)到结束日期(含)的所有日期,注意开始和结束时间先用{@link #timeThreshold}计算一遍<br>
     * 最终单位以granularityUnit指定为准,如<br>
     * ⊙granularityUnit={@link java.time.temporal.ChronoUnit#MONTHS 月}
     *   ，则从2016-12-31 00:00:00~20170102 00:00:00=[201612,201701]
     * ⊙granularityUnit={@link java.time.temporal.ChronoUnit#DAYS 日}
     *   ，则从2017-07-01 00:00:00~20170703 00:00:00=[20170701,20170702,20170703]
     * ⊙granularityUnit={@link java.time.temporal.ChronoUnit#HOURS 时}
     *   ，则从2017-07-01 00:00:00~20170701 02:00:00=[2017070100,2017070101,2017070102]
     */
    public static List<Integer> granularities(Date from, Date to, ChronoUnit granularityUnit) {
        List<Integer> granularities = new LinkedList<Integer>();
        if(to.compareTo(from)<0) return granularities;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        do{
            granularities.add(granularityStamp(calendar.getTime(), granularityUnit));
            switch (granularityUnit) {
                case MONTHS:
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)+1);
                    break;
                case DAYS:
                    calendar.set(Calendar.DATE, calendar.get(Calendar.DATE)+1);
                    break;
                case HOURS:
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY)+1);
                    break;
                default:
                    throw new Err("不支持的时间粒度:"+granularityUnit);
            }
        }while(calendar.getTime().compareTo(to)<=0);
        return granularities;
    }
    @SneakyThrows
    public static List<Integer> granularities(int from, int to, ChronoUnit granularityUnit) {
        Date fromDate,toDate;
        String format = granularityFormat(granularityUnit);
        fromDate = DateUtils.parseDateStrictly(String.valueOf(from), format);
        toDate = DateUtils.parseDateStrictly(String.valueOf(to), format);
        return granularities(fromDate, toDate, granularityUnit);
    }
    /** 格式化时间为十位的小时戳或八位的日期戳或者六位的月份戳 */
    public static int granularityStamp(Date date, ChronoUnit granularityUnit) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return granularityStamp(calendar, granularityUnit);
    }
    /** 格式化时间为十位的小时戳或八位的日期戳或者六位的月份戳 */
    public static int granularityStamp(long millis, ChronoUnit granularityUnit) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return granularityStamp(calendar, granularityUnit);
    }
    /** 格式化时间为十位的小时戳或八位的日期戳或者六位的月份戳 */
    public static int granularityStamp(Calendar calendar, ChronoUnit granularityUnit) {
        int granularityStamp = calendar.get(Calendar.YEAR);
        granularityStamp = granularityStamp*100+calendar.get(Calendar.MONTH)+1;
        if(ChronoUnit.MONTHS==granularityUnit) return granularityStamp;
        granularityStamp = granularityStamp*100+calendar.get(Calendar.DATE);
        if(ChronoUnit.DAYS==granularityUnit) return granularityStamp;
        granularityStamp = granularityStamp*100+calendar.get(Calendar.HOUR_OF_DAY);
        return granularityStamp;
    }
    public static String granularityFormat(ChronoUnit granularityUnit) {
        switch (granularityUnit) {
            case MONTHS: return "yyyyMM";
            case DAYS: return "yyyyMMdd";
            case HOURS: return "yyyyMMddHH";
            default: throw new Err("不支持的时间粒度:"+granularityUnit);
        }
    }
    
    /**
     * 判断to是否为from的下一月
     * @param from 用六位整形数字表示的月份，如201704
     * @param to 用六位整形数字表示的月份，如201704
     */
    public static boolean isNextMonth(int from, int to) {
        Calendar calendar = Calendar.getInstance();
        int fromYear = from/100;
        int fromMonth = from%100;
        calendar.set(fromYear, fromMonth, 1);
        int tmp =  calendar.get(Calendar.YEAR)*100+(calendar.get(Calendar.MONTH)+1);
        return tmp==to;
    }
    
    /** 使用最常用的日期格式yyyy-MM-dd做格式化 */
    public static final String dayFormat(Date date) {
        if(date==null) return null;
        return DateFormatUtils.format(date, "yyyy-MM-dd");
    }
    /** 将当前时间使用最常用的日期格式yyyy-MM-dd HH:mm:ss做格式化 */
    public static final String dateFormat() {
        return DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
    }
    /** 使用最常用的日期格式yyyy-MM-dd HH:mm:ss做格式化 */
    public static final String dateFormat(Date date) {
        if(date==null) return null;
        return DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
    }
    /** 使用最常用的日期格式yyyy-MM-dd HH:mm:ss做时间解析,隐藏异常 */
    @SneakyThrows
    public static final Date dateParse(String date) {
        return DateUtils.parseDate(date, "yyyy-MM-dd HH:mm:ss");
    }
    /** 使用最常用的日期格式yyyy-MM-dd HH:mm:ss做格式化 */
    public static final String dateFormat(Long date) {
        if(date==null) return null;
        return DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
    }
    /** 时长格式化为00:00:00 */
    public static String timeFormat(Long time) {
        if(time==null) return null;
        time = time/1000;
        int s = (int) (time%60);
        time = time/60;
        int m = (int) (time%60);
        time = time/60;
        int h = (int) (time%60);
        StringBuilder sb = new StringBuilder();
        if(h<10) sb.append("0"); sb.append(h+":");
        if(m<10) sb.append("0"); sb.append(m+":");
        if(s<10) sb.append("0"); sb.append(s+":");
        return sb.toString();
    }
    
    /** 计算从开始天到结束天的天数总数,参数必须是八位日期戳,开始与结束日期包含在内 */
    @SneakyThrows
    public static int daysCount(int from, int to) {
        long fromTime = DateUtils.parseDate(String.valueOf(from), "yyyyMMdd").getTime();
        long toTime = DateUtils.parseDate(String.valueOf(to), "yyyyMMdd").getTime();
        long daysCount = (toTime-fromTime)/(24*60*60*1000L);
        if(daysCount<0) return 0;
        return (int) (daysCount+1);
    }
    /** 在一个八位日期戳上加上dayNum后得到的新日期戳 */
    public static int dayStampAdd(int dayStamp, int dayNum) {
        int year = dayStamp/10000;
        dayStamp = dayStamp%10000;
        int month = dayStamp/100;
        int date = dayStamp%100;
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month-1, date+dayNum);
        return granularityStamp(calendar, ChronoUnit.DAYS);
    }
    
    /**
     * 时间阈值调整<br>
     * 开始时间调整为一天的最开始(00:00:00,000)<br>
     * 结束时间调整为一天的最末尾(23:59:59,999)
     */
    public static Date timeThreshold(boolean beginOrEnd, ChronoUnit granularityUnit, Date time) {
        //开始时间调整为一天的最开始(00:00:00,000)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        if(beginOrEnd) { //开始时间
            switch (granularityUnit) {
                case MONTHS:
                    calendar.set(Calendar.DATE, 1); //月粒度,调整为当月第一天
                case DAYS:
                    calendar.set(Calendar.HOUR_OF_DAY, 0); //天粒度,调整为0点
                case HOURS:
                    calendar.set(Calendar.MINUTE, 0); //时粒度,调整为0分
                default:
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    break;
            }
        } else { //结束时间
            switch (granularityUnit) {
                case MONTHS:
                    calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE)); //月粒度,调整为当月最后一天
                case DAYS:
                    calendar.set(Calendar.HOUR_OF_DAY, 23); //天粒度,调整为23点
                case HOURS:
                    calendar.set(Calendar.MINUTE, 59); //时粒度,调整为59分
                default:
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 999);
                    break;
            }
        }
        return calendar.getTime();
    }
    /** 时间阈值调整 */
    @SneakyThrows
    public static Date timeThreshold(boolean beginOrEnd, ChronoUnit granularityUnit, int time) {
        String format = granularityFormat(granularityUnit);
        Date date = DateUtils.parseDateStrictly(String.valueOf(time), format);
        return timeThreshold(beginOrEnd, granularityUnit, date);
    }
    
    /**
     * 教务系统需要用到的线程池，基本上需求都是空闲时不保持线程<br>
     * 对最大线程数量、任务队列和拒绝策略有自定义要求的线程池<br>
     * 本方法稍微封装下线程池的初始化
     * @param name 线程池中所有线程名字的前缀
     * @param maximumPoolSize
     * @param workQueue
     * @param handler
     * @return
     */
    public static ThreadPoolExecutor newThreadPoolExecutor(String name, int maximumPoolSize,
            BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize,
                1, MINUTES, workQueue, namedThreadFactory(name), handler);
        threadPool.allowCoreThreadTimeOut(true);
        return threadPool;
    }
    /** 创建一个自定义名字的线程池 */
    public static ThreadFactory namedThreadFactory(String namePrefix) {
        AtomicInteger poolNumber = new AtomicInteger();
        return (r)->{
            Thread t = new Thread(r, namePrefix+"_"+poolNumber.getAndIncrement());
            if(t.isDaemon()) t.setDaemon(false);
            if(NORM_PRIORITY!=t.getPriority()) t.setPriority(NORM_PRIORITY);
            return t;
        };
    }
    
    
    /** 辅助stream处理jsonarray为List&lt;String&gt,null返回空list */
    public static List<String> JsonArray2Strs(JSONArray jsonArray) {
        if(CollectionUtils.isEmpty(jsonArray)) return new ArrayList<>();
        return IntStream.range(0, jsonArray.size()).mapToObj(jsonArray::get)
            .map(o->o==null||JSONNull.getInstance()==o?null:String.valueOf(o))
            .collect(toList());
    }
    /** 辅助stream处理jsonarray为List&lt;Integer&gt,null返回空list */
    public static List<Integer> JsonArray2Ints(JSONArray jsonArray) {
        if(CollectionUtils.isEmpty(jsonArray)) return new ArrayList<>();
        return JsonArray2T(jsonArray, jsonArray::getInt);
    }
    /** 辅助stream处理jsonarray为指定对象列表列表 */
    public static <T> List<T> JsonArray2T(JSONArray jsonArray, IntFunction<T> function) {
        if(CollectionUtils.isEmpty(jsonArray)) return new ArrayList<>();
        return IntStream.range(0, jsonArray.size()).mapToObj(function).collect(toList());
    }
    
    /** 从数组，集合，迭代器等中拿取指定第几个元素，若不存在或越界，返回null */
    public static <T> T collectionGet(Object collection, int index) {
        return collectionGet(collection, index, null);
    }
    /** 从数组，集合，迭代器等中拿取指定第几个元素，若不存在或越界，返回默认值 */
    @SuppressWarnings("unchecked")
    public static <T> T collectionGet(Object collection, int index, T defaultValue) {
        try{
            if(collection==null) return defaultValue;
            return (T) CollectionUtils.get(collection, index);
        }catch(Exception e){
            return defaultValue;
        }
    }
    
    /** 辅助Stream隐藏异常,默认遇到异常抛出运行时异常 */
    public static <T> Predicate<T> predicate(IgPredicate<T> predicate) {
        return t -> {
            try{
                return predicate.test(t);
            }catch (Throwable e){
                throw new RuntimeException(e);
            }
        };
    }
    /** 辅助Stream隐藏异常,默认遇到异常抛出为运行时异常 */
    public static <T, R> Function<T, R> function(IgFunction<T, R> function) {
        return function(function, true);
    }
    /** 辅助Stream隐藏异常,可配置遇到异常返回null或抛出运行时异常 */
    public static <T, R> Function<T, R> function(IgFunction<T, R> function, boolean throwAsRuntimeException) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Throwable e) {
                if(throwAsRuntimeException) throw new RuntimeException(e);
                return null;
            }
        };
    }
    /** 辅助LongStream隐藏异常,默认遇到异常抛出为运行时异常 */
    public static <R> LongFunction<R> longFunc(IgLongFunction<R> func) {
        return longFunc(func, true);
    }
    /** 辅助LongStream隐藏异常,可配置遇到异常返回null或抛出运行时异常 */
    public static <R> LongFunction<R> longFunc(IgLongFunction<R> func, boolean throwAsRuntimeException) {
        return t -> {
            try{
                return func.apply(t);
            }catch(Throwable e){
                if(throwAsRuntimeException) throw new RuntimeException(e);
                return null;
            }
        };
    }
    /** 辅助Stream隐藏异常,默认遇到异常抛出为运行时异常 */
    public static <T> Consumer<T> consumer(IgComsumer<T> comsumer) {
        return consumer(comsumer, true);
    }
    /** 辅助Stream隐藏异常,可配置是否抛出为运行时异常 */
    public static <T> Consumer<T> consumer(IgComsumer<T> comsumer, boolean throwAsRuntimeException) {
        return t -> {
            try {
                comsumer.accept(t);
            } catch (Throwable e) {
                if(throwAsRuntimeException) throw new RuntimeException(e);
            }
        };
    }
    
    public interface IgPredicate<T> {
        boolean test(T t) throws Throwable;
    }
    public interface IgFunction<T, R> {
        R apply(T t) throws Throwable;
    }
    public interface IgLongFunction<R> {
        R apply(Long t) throws Throwable;
    }
    public interface IgComsumer<T> {
        void accept(T t) throws Throwable;
    }
    
    /**
     * 代码重试机制，运行retryFunction(至少一次)，默认3次重试，3次重试间无间隔
     * @param <R>
     * @param retryFunction
     * @return 返回retryFunction的返回
     * @throws 最后一次异常
     */
    public static <R> R retry(RetryFunction<R> retryFunction) throws Exception {
        return retry(retryFunction, (e,n)->n<3);
    }
    /**
     * 代码重试机制，运行retryFunction(至少一次)，若出现异常在满足retryPredicate条件下重试
     * @param <R>
     * @param retryFunction
     * @param retryPredicate
     * @return 返回retryFunction的返回
     * @throws 不满足retryPredicate条件时的最后一次异常(或retryPredicate本身抛出的异常)
     */
    public static <R> R retry(RetryFunction<R> retryFunction, RetryPredicate retryPredicate) throws Exception {
        int executionCount = 0;
        Exception e;
        do{
            try{
                return retryFunction.run();
            }catch(Exception e2){
                e = e2;
            }
            executionCount += 1;
        }while(retryPredicate.retry(e, executionCount));
        throw e;
    }
    /**
     * 代码重试机制，运行retryRunnable(至少一次)，默认3次重试，3次重试间无间隔
     * @param retryRunnable
     * @param retryPredicate
     * @throws 最后一次异常
     */
    public static void retry(RetryRunnable retryRunnable) throws Exception {
        retry(retryRunnable, (e,n)->n<3);
    }
    /**
     * 代码重试机制，运行retryRunnable(至少一次)，若出现异常在满足retryPredicate条件下重试
     * @param retryRunnable
     * @param retryPredicate
     * @throws 不满足retryPredicate条件时的最后一次异常(或retryPredicate本身抛出的异常)
     */
    public static void retry(RetryRunnable retryRunnable, RetryPredicate retryPredicate) throws Exception {
        int executionCount = 0;
        Exception e;
        do{
            try{
                retryRunnable.run();
                return;
            }catch(Exception e2){
                e = e2;
            }
            executionCount += 1;
        }while(retryPredicate.retry(e, executionCount));
        throw e;
    }
    public interface RetryFunction<R> {
        R run() throws Exception;
    }
    public interface RetryRunnable {
        void run() throws Exception;
    }
    public interface RetryPredicate {
        boolean retry(Throwable e, int executionCount) throws Exception;
    }
}
