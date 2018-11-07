package com.lvt4j.im;

import static com.lvt4j.im.Consts.AppFolder;
import static com.lvt4j.im.Consts.TmpFolder;

import java.io.File;
import java.net.ServerSocket;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPidFileWriter;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author LV
 */
@Slf4j
@SpringBootApplication
public class Main{

    private static final String BannerTpl = "\n"
        +"===      Instant Message      ===\n"
        +"== App路径：%s\n"
        +"== Tmp路径：%s\n"
        ;
    
    public static void main(String[] args) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.io.tmpdir", TmpFolder.getAbsolutePath());
        
        checkPortAlreadyInUse(Config.port, "节点配置[port]端口["+Config.port+"]被其他服务占用，无法启动");
        checkPortAlreadyInUse(Config.httpPort, "Spring配置[server.port]端口["+Config.httpPort+"]被其他服务占用，无法启动");
        if(Config.bindPort!=null) checkPortAlreadyInUse(Config.bindPort, "节点配置[node.bind-port]端口["+Config.bindPort+"]被其他服务占用，无法启动");
        
        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanTmp));
        
        SpringApplication app = new SpringApplication(Main.class);
        app.setBanner((e, c, o)->o.print(String.format(BannerTpl,
                AppFolder.getAbsolutePath(), TmpFolder.getAbsolutePath())));
        File pidFile = new File(AppFolder, "applicationPid");
        pidFile.createNewFile();
        pidFile.deleteOnExit();
        app.addListeners(new ApplicationPidFileWriter(pidFile));
        Global.SpringCtx = app.run();
    }
    
    private static void checkPortAlreadyInUse(int port, String errMsg) {
        try{
            new ServerSocket(port).close();
        }catch(Throwable e){
            log.error(errMsg);
            throw new Error(errMsg, e);
        }
    }
    
    @SneakyThrows
    private static void cleanTmp() {
        while(Global.SpringCtx!=null && Global.SpringCtx.isActive()){ Thread.sleep(1000); }
        try{
            FileUtils.deleteDirectory(TmpFolder);
            log.info("清理临时文件夹成功!");
        }catch(Throwable e){
            log.error("清理临时文件夹失败!", e);
        }
    }
    
}
