package com.lvt4j.im;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 各种全局常量
 * @author LV
 */
public class Consts {

    /** App名 */
    public static final String AppName = "IM";
    
    /** 扫描类时用的类加载器 */
    public static final ClassLoader ClassLoader = Consts.class.getClassLoader();
    
    /** 根包包名 */
    public static final String BasePackage = Consts.class.getPackage().getName();
    /** 根包包路径 */
    public static final String BasePackagePath = BasePackage.replaceAll("[.]", "/");
    /** 启动时间 */
    public static final long StartTime = System.currentTimeMillis();
    
    /** 前端资源版本 */
    public static final AtomicLong ResVer = new AtomicLong(System.currentTimeMillis());
    
    /** app根文件夹 */
    public static final File AppFolder = new File(System.getProperty("user.dir"));
    /** config文件夹 */
    public static final File ConfFolder = new File(AppFolder, "config");
    /** application.properties配置文件 */
    public static final File AppConfFile = new File(ConfFolder, "application.properties");
    /** logback配置文件 */
    public static final File LogbackFile = new File(ConfFolder, "logback.xml");
    /** 临时文件夹,用于生成临时文件 */
    public static final File TmpFolder = new File(AppFolder, "tmp");
    /** web文件夹 */
    public static final File WebFolder = new File(AppFolder, "web");
    
    static{
        TmpFolder.mkdirs();
    }
}
