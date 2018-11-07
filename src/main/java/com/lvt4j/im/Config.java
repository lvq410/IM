package com.lvt4j.im;

import static com.lvt4j.im.Consts.AppConfFile;
import static com.lvt4j.im.Consts.LogbackFile;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import lombok.Getter;

/**
 * @author LV
 */
@Getter
@ConfigurationProperties
public class Config {

    //=================================================================App用配置
    private static final PropertiesConfiguration Props;
    
    /** http端口 */
    public static final int httpPort;
    /** 本节点主机 */
    public static final String host;
    /** 本节点端口 */
    public static final int port;
    /** 绑定主机名 */
    public static final String bindHost;
    /** 绑定端口 */
    public static final Integer bindPort;
    
    static{
        try{
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            loggerContext.reset();
            configurator.doConfigure(LogbackFile);
        }catch(Exception e){
            throw new Error("加载日志配置文件"+LogbackFile.getAbsolutePath()+"失败！", e);
        }
        try {
            Props = new PropertiesConfiguration();
            Props.setEncoding("utf-8");
            Props.setFile(AppConfFile);
            Props.setReloadingStrategy(new FileChangedReloadingStrategy());
            Props.load();
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件application.properties失败！", e);
        }
        httpPort = Props.getInt("server.port");
        host = Props.getString("node.host");
        port = Props.getInt("node.port");
        bindHost = Props.getString("node.bind-host");
        bindPort = Props.getInteger("node.bind-port", null);
    }
    
    public static final String[] Nodes(){return Props.getStringArray("nodes");}
    public static final int Quorum(){return Nodes().length/2+1;}
    
}
