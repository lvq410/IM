package com.lvt4j.im;
import static com.lvt4j.im.Consts.BasePackage;
import static com.lvt4j.im.Consts.ClassLoader;
import static com.lvt4j.im.common.Utils.function;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
/**
 * 核心配置，包括<br>
 * 数据源
 * @author LV
 */
@Configuration
@EnableConfigurationProperties
public class SpringCoreConfig implements ApplicationContextAware {

    @Bean
    public Config config() {
        return new Config();
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        if(Global.SpringCtx==null) Global.SpringCtx = (ConfigurableApplicationContext) applicationContext;
    }
    
    /** 扫描{@link outfox.courseop.Consts.BasePackage BasePackage}下的所有类 */
    private static List<Class<?>> scanCls() {
        return new FastClasspathScanner(BasePackage).scan()
                .getNamesOfAllClasses().stream()
                .map(function(ClassLoader::loadClass, false))
                .filter(Objects::nonNull)
            .collect(toList());
    }
    /** 扫描{@link outfox.courseop.Consts.BasePackage BasePackage}下的所有继承或实现了tCls的非(接口或抽象)的类 */
    @SuppressWarnings("unchecked")
    public static <T> List<Class<T>> scanAssignableClss(Class<T> tCls) {
        return scanCls().stream()
                .map(c->(Class<T>)c)
                .filter(tCls::isAssignableFrom)
                .filter(cls->!cls.isInterface())
                .filter(cls->!Modifier.isAbstract(cls.getModifiers()))
            .collect(toList());
    }
    /** 扫描{@link outfox.courseop.Consts.BasePackage BasePackage}下的所有继承或实现了tCls的非(接口或抽象)的类
     * ，并返回每个类的无参构造函数构造出的实例 */
    public static <T> List<T> scanAssignables(Class<T> tCls) {
        return scanAssignableClss(tCls).stream()
            .map(function(Class::newInstance, true))
            .map(tCls::cast)
        .collect(toList());
    }
    
}
