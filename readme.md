一个基于akka的分布式即时通讯（Instant Message）服务Demo

# 启动方法 #

下载[demo](http://t.cn/EAPD9c3)包，解压后
```
//修改配置文件
vim ./config/application.properties
node.host=localhost //单机模式不用改，多机器集群模式为主机名/IP
nodes=localhost:9002 //单机模式不用改，多机器集群模式为所有集群成员的主机名/IP:node.port
//需要Java8环境，如有必要，需修改./run/start.sh增加Java8环境变量配置
sh ./run/start.sh 启动
```
# 使用方法 #
## 会话页 ##
http://localhost:9000/index.html?userId=b

注意这是个Demo，需要一个userId来表名用户……

## 状态查看页 ##
http://localhost:9000/status.html

这个页面可以查看集群成员及每个成员正在服务的用户情况
