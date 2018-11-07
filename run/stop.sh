#!/bin/sh
set -e
shellDir=`dirname $0`
cd $shellDir/..
if [ ! -f "./applicationPid" ]; then
    echo '服务早已停止!'
else
    pid=`cat ./applicationPid`
    echo '正在关停服务进程['$pid']中,部分线程池处理剩余任务可能耗时较长,请耐心等待'
    kill $pid
    rm ./applicationPid
    echo '已关停服务运行的进程'$pid
fi
