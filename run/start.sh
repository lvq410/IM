#!/bin/sh
set -e
#切换至工作目录
shellDir=`dirname $0`
cd $shellDir/..
shellDir=`pwd`
echo '项目路径：'$shellDir
#检查重复启动
if [ -f "./applicationPid" ]; then
    ePid=`cat ./applicationPid`
    echo '检测到服务已运行于进程'$ePid'上,结束启动命令!'
    echo '若并无该进程运行，请删除文件./applicationPid后重试'
    exit 0
fi
#配置java命令及参数
JAVA_EXE=$JAVA_HOME/bin/java
args="-Dfile.encoding=utf-8
    -Xmx3210m
    -Xmn1024m
    -XX:+UseParNewGC
    -XX:+UseConcMarkSweepGC
    -XX:CMSInitiatingOccupancyFraction=80
    -XX:+PrintGCDetails
    -XX:+PrintGCDateStamps
    -XX:+PrintGCTimeStamps
    -XX:+PrintHeapAtGC
    -Xloggc:logs/gc.log
    -XX:PermSize=64M
    -XX:MaxNewSize=1256m
    -XX:MaxPermSize=128m"
#附加javaDebug信息
debugPort=`awk -F '=' '{if($1=="debug.port"){print $2;}}' ./config/application.properties`
if [ -n "$debugPort" ]; then 
    args=$args" -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address="$debugPort
fi
$JAVA_EXE $args -jar $shellDir/im.jar >/dev/null 2>&1 &
#解析akka端口信息
akkaPort=`awk -F '=' '{if($1=="node.port"){print $2;}}' ./config/application.properties`
#解析服务端口信息
serverPort=`awk -F '=' '{if($1=="server.port"){print $2;}}' ./config/application.properties`
#等待5秒以保证应用进程id文件创建出来
sleep 5
if [ -f "./applicationPid" ]; then
    for((i=1;i<=12;i++));
    do
        pid=`cat ./applicationPid`
        if [ -z "$pid" ]; then
            echo '等待进程ID生成中，已等待'$[i*5]'s，最多等待60s'
            sleep 5s
        else
            break
        fi
    done
    if [ -z "$pid" ]; then
        echo '服务启动失败！等待60s超时仍未生成进程ID'
    else
        echo '启动成功!服务运行于进程:'$pid
    fi
else
    echo '服务启动失败!'
    echo '进程文件applicationPid不存在!请检查是否有端口冲突'
fi
#打印端口号
echo 'akka端口:'$akkaPort
echo 'http端口:'$serverPort
if [ -n "$debugPort" ]; then 
    echo 'jvm debug端口:'$debugPort
fi
