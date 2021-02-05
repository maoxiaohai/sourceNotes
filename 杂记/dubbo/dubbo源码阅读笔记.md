## 1.环境搭建
1. 下载源码：git clone --branch 2.6.x https://gitee.com/XHmao/mydubbo.git
  下载的是2.6.x版本的
2. 源码下载后直接导入idea编译器
3. 修改配置文件   
 3.1 修改dubbo-demo-provider模块配置文件(dubbo-demo-provider.xml)
  
  provider模块需要修改两处配置，一处是将注册中心修改为zk
```
<dubbo:registry address="zookeeper://127.0.0.1:2181"/>
```
  第二个地方是修改暴露出去的服务端口
  ```
  <dubbo:protocol name="dubbo" port="20881"/>
  ```

3.2修改dubbo-demo-consumer模块配置文件(dubbo-demo-consumer.xml)
```
 <dubbo:registry address="zookeeper://127.0.0.1:2181"/>
```
consumer只需要修改注册中心为zk即可
配置完之后，首先启动zk，然后依次启动provider和consumer就ok