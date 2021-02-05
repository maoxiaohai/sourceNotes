## 消费者

dubbo消费者示例代码为：

```java
public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);
                String hello = demoService.sayHello("world"); // call remote method
                System.out.println(hello); // get result

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

    }
```

##### demoService是接口，是需要调用服务器的接口，消费者端是没有实现的，这怎么从spring容器中拿出来？而且还能用DemoService类型接住。

```xml

<dubbo:reference id="demoService" check="false" interface="com.alibaba.dubbo.demo.DemoService"/>
```

在配置文件中，可以看到rpc调用的接口是通过以上的节点配置的。

看到DubboNamespaceHandler类中是这样的：

```java
@Override
    public void init() {
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        //消费者端的引用
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false));
        registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser());
    }
```

可以看到```<dubbo:reference>```节点最后是被实例化为ReferenceBean对象的。通过代码追踪，发现spring容器中也确实是ReferenceBean对象，并且，ReferenceBean实例的```id=demoService```,那么，```context.getBean("demoService")```取出来的对象为啥还是DemoService类型呢？

通过跟踪```context.getBean("demoService")```方法，发现原来ServiceBean实现了FactoryBean接口，在获取bean的时候，会使用自定义实现的FactoryBean(这里就是ServiceBean)去获取bean。

查看Reference类中的getObject().

```
	 @Override
    public Object getObject() throws Exception {
        return get();
    }
```

最终创建DemoService实例的地方在ReferenceConfig类的createProxy方法中。

```java

ReferenceBean	
```



## ProxyFactory&Adaptive

如下是ProxyFactory代理类

```java
package com.alibaba.dubbo.rpc;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
public class ProxyFactory$Adaptive implements com.alibaba.dubbo.rpc.ProxyFactory {

public java.lang.Object getProxy(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.RpcException {
    if (arg0 == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
    if (arg0.getUrl() == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");com.alibaba.dubbo.common.URL url = arg0.getUrl();
    String extName = url.getParameter("proxy", "javassist");
    if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.ProxyFactory) name from url(" + url.toString() + ") use keys([proxy])");
    com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class).getExtension(extName);
    return extension.getProxy(arg0);
}


public java.lang.Object getProxy(com.alibaba.dubbo.rpc.Invoker arg0, boolean arg1) throws com.alibaba.dubbo.rpc.RpcException {
    if (arg0 == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
    if (arg0.getUrl() == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");com.alibaba.dubbo.common.URL url = arg0.getUrl();
    String extName = url.getParameter("proxy", "javassist");
    if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.ProxyFactory) name from url(" + url.toString() + ") use keys([proxy])");
    com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class).getExtension(extName);
    return extension.getProxy(arg0, arg1);
}


public com.alibaba.dubbo.rpc.Invoker getInvoker(java.lang.Object arg0, java.lang.Class arg1, com.alibaba.dubbo.common.URL arg2) throws com.alibaba.dubbo.rpc.RpcException {
    if (arg2 == null) throw new IllegalArgumentException("url == null");
    com.alibaba.dubbo.common.URL url = arg2;
    String extName = url.getParameter("proxy", "javassist");
    if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.ProxyFactory) name from url(" + url.toString() + ") use keys([proxy])");

    com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class).getExtension(extName);
    return extension.getInvoker(arg0, arg1, arg2);
	}
}
```

如果在spi配置文件中有adaptive=xxxx的配置，说明dubbo已经给出了适配类，否则会自行代码生成相应的adaptive类。该适配类的作用就是适配出相应的具体实现，如何确定对应的实现呢？通过参数中的url或者invoke中的getUrl()方法，注意这里不同的invoke返回的url会不一样的。通过返回的url的参数protocol确定最终的实现类。并调用实现类去执行对应的```getInvoker()```或者```getProxy()```方法。



## 消费端方法调用

以下是consumer.demo中的provider方法调用

```java
public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        //返回的是代理对象
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy
        //返回的是代理对象
        Demo2 demo2=(Demo2)context.getBean("demo2Service");
        demo2.method();
        while (true) {
            try {
                Thread.sleep(1000);
                String hello = demoService.sayHello("world"); // call remote method
                System.out.println(hello); // get result

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

    }
```

consumer段获取到的接口实例都是代理对象[此处代理对象为MockClusterInvoker(FailoverClusterInvoker的包装类，MockClusterInvoker中的invoker实例就是FailoverClusterInvoker)]。

代理对象调用接口方法```demo2.method()```时，~~实例调用的是代理对象的invoke方法【在实例化代理对象时，加入了InvokerInvocationHandler】~~。

```java
 public Result invoke(Invocation invocation) throws RpcException {
        Result result = null;

        String value = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY, Boolean.FALSE.toString()).trim();
        if (value.length() == 0 || value.equalsIgnoreCase("false")) {
            //no mock  //invoker就是被包装的FailoverClusterInvoker实例，所以实际上执行的还是FailoverClusterInvoker的invoke方法
            result = this.invoker.invoke(invocation);
        } else if (value.startsWith("force")) {
            if (logger.isWarnEnabled()) {
                logger.info("force-mock: " + invocation.getMethodName() + " force-mock enabled , url : " + directory.getUrl());
            }
            //force:direct mock
            result = doMockInvoke(invocation, null);
        } else {
            //fail-mock
            try {
                result = this.invoker.invoke(invocation);
            } catch (RpcException e) {
                if (e.isBiz()) {
                    throw e;
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn("fail-mock: " + invocation.getMethodName() + " fail-mock enabled , url : " + directory.getUrl(), e);
                    }
                    result = doMockInvoke(invocation, e);
                }
            }
        }
        return result;
    }
```



在获取bean对象的时候```context.getBean("demo2Service");```,其实返回的是一个代理对象，代理对象通过```getProxy```返回。

```java
 public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
    return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
  }
```

其中，``` Proxy.getProxy(interfaces)```返回的类如下所示：

```java
package com.alibaba.dubbo.common.bytecode;

import com.alibaba.dubbo.common.bytecode.ClassGenerator.DC;
import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.rpc.service.EchoService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class proxy0 implements DC, EchoService, DemoService {
  public static Method[] methods;
  private InvocationHandler handler;

  public String sayHello(String var1) {
    Object[] var2 = new Object[]{var1};
    Object var3 = this.handler.invoke(this, methods[0], var2);
    return (String)var3;
  }

  public Object $echo(Object var1) {
    Object[] var2 = new Object[]{var1};
    Object var3 = this.handler.invoke(this, methods[1], var2);
    return (Object)var3;
  }

  public proxy0() {
  }

  public proxy0(InvocationHandler var1) {
    this.handler = var1;
  }
}
```

可看出来，dubbo是通过继承的方式实现的代理，当调用具体方法时，实际调用的是```handler.invoke```,而handel通过```newInstance(InvocationHandler handler)```来传入。



执行顺序

#### InvokerInvocationHandler->invoke

```java

InvokerInvocationHandler.invoke(...){
    //invoker为MockClusterInvoker
    return invoker.invoke(new RpcInvocation(method,args)).recreate();
}
```

#### MockClusterInvoker->invoke

```java
public Result invoke(Invocation invocation) throws RpcException {
	try {
	   //invoker为FailoverClusterInvoker
        result = this.invoker.invoke(invocation);
      } 
      return result;
}

```

#### FailoverClusterInvoker->invoke[FailoverClusterInvoker未实现invoke方法，调用其父类AbstractClusterInvoker]

```java
 public Result invoke(final Invocation invocation) throws RpcException {
 ...
     //子类实现具体的doInvoke()
	 return doInvoke(invocation, invokers, loadbalance);
 }
```

#### FailoverClusterInvoker->doInvoke

```java
public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
	...
	//负载均衡选择
	Invoker<T> invoker = select(loadbalance, invocation, copyinvokers, invoked);
	...
	//invoker=RegistryDirectory$InvokerDelegate
	 Result result = invoker.invoke(invocation);
	 return result;
}
```

#### RegistryDirectory$InvokerDelegate->invoke【为具体实现，调用其父类InvokerWrapper.invoke】

```java
  public Result invoke(Invocation invocation) throws RpcException {
      //ProtocolFilterWrapper
    return invoker.invoke(invocation);
  }
```

#### ProtocolFilterWrapper->invoke

```
 public Result invoke(Invocation invocation) throws RpcException {
     //filter= ConsumerContextFilter
     return filter.invoke(next, invocation);
  }
```



#### ConsumerContextFilter.invoke

```
public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
  ....
	 try {
	 //invoker=ProtocolFilterWrapper
      RpcResult result = (RpcResult) invoker.invoke(invocation);
      RpcContext.getServerContext().setAttachments(result.getAttachments());
      return result;
    } finally {
      RpcContext.getContext().clearAttachments();
    }
}
```

#### 再次ProtocolFilterWrapper->invoke

```java
 public Result invoke(Invocation invocation) throws RpcException {
     //filter= FutureFilter
     return filter.invoke(next, invocation);
  }
```

#### FutureFilter->invoke

```
public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {
    ...
    //invoker=ProtocolFilterWrapper
 	Result result = invoker.invoke(invocation);
    if (isAsync) {
      asyncCallback(invoker, invocation);
    } else {
      syncCallback(invoker, invocation, result);
    }
    return result;
}
```

#### 再次执行ProtocolFilterWrapper->invoker

```java
public Result invoke(Invocation invocation) throws RpcException {
  //filter=MonitorFilter
  return filter.invoke(next, invocation);
}
```

#### MonitorFilter->invoke

```java
public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
	...
	  //ListenerInvokerWrapper
	  return invoker.invoke(invocation);
}
```



#### ListenerInvokerWrapper->invoke

```
 public Result invoke(Invocation invocation) throws RpcException {
    //invoker=DubboInvoker
    return invoker.invoke(invocation);
  }
```

#### DubboInvoker->invoke【DubboInvoker未实现invoke，调用其父类AbstracInvoker.invoke】

```
 public Result invoke(Invocation inv) throws RpcException {
     //调用其子类具体实现[DubboInvoker]
	 return doInvoke(invocation);
 }
```

#### DubboInvoker.doInvoke

```
protected Result doInvoke(final Invocation invocation) throws Throwable {
	...
	//currentClient->ReferenceCountExchangeClient
	 return (Result) currentClient.request(inv, timeout).get();
}
```

#### ReferenceCountExchangeClient.request

```
 public ResponseFuture request(Object request, int timeout) throws RemotingException {
   //client->HeaderExchangeClient
   return client.request(request, timeout);
  }
```

#### HeaderExchangeClient->request

```
public ResponseFuture request(Object request, int timeout) throws RemotingException {
   //HeaderExchangeChannel
   return channel.request(request, timeout);
  }
```

#### HeaderExchangeChannel->request

```
public ResponseFuture request(Object request, int timeout) throws RemotingException {
    if (closed) {
      throw new RemotingException(this.getLocalAddress(), null, "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
    }
    // create request.
    Request req = new Request();
    req.setVersion(Version.getProtocolVersion());
    req.setTwoWay(true);
    req.setData(request);
    //*************************************************
    //DefaultFuture有个静态代码块，在静态代码块中会执行一个RemotingInvocationTimeoutScan的线程,并且线程中是一个死循环
    DefaultFuture future = new DefaultFuture(channel, req, timeout);
    try {
      channel.send(req);
    } catch (RemotingException e) {
      future.cancel();
      throw e;
    }
    return future;
  }
```



#### RemotingInvocationTimeoutScan

```
  public void run() {
      while (true) {
        try {
          for (DefaultFuture future : FUTURES.values()) {
          //如果结果没有返回，则继续等待。future.isDone()就是判断response是否为空
            if (future == null || future.isDone()) {
              continue;
            }
            if (System.currentTimeMillis() - future.getStartTimestamp() > future.getTimeout()) {
              // create exception response.
              Response timeoutResponse = new Response(future.getId());
              // set timeout status.
              timeoutResponse.setStatus(future.isSent() ? Response.SERVER_TIMEOUT : Response.CLIENT_TIMEOUT);
              timeoutResponse.setErrorMessage(future.getTimeoutMessage(true));
              // handle response. 开始接受返回结果
              DefaultFuture.received(future.getChannel(), timeoutResponse);
            }
          }
          Thread.sleep(30);
        } catch (Throwable e) {
          logger.error("Exception when scan the timeout invocation of remoting.", e);
        }
      }
    }
```



#### 获取结果:DefaultFuture->get

```
  public Object get(int timeout) throws RemotingException {
    if (timeout <= 0) {
      timeout = Constants.DEFAULT_TIMEOUT;
    }
    if (!isDone()) {
      long start = System.currentTimeMillis();
      lock.lock();
      try {
        //没有结果返回
        while (!isDone()) {
          //没有结果返回，等待timeout的时间
          done.await(timeout, TimeUnit.MILLISECONDS);
          if (isDone() || System.currentTimeMillis() - start > timeout) {
            break;
          }
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        lock.unlock();
      }
      if (!isDone()) {
        throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
      }
    }
    return returnFromResponse();
  }

```



### 资源加载记录

```java
ExtensionLoader.class.getClassLoader().getResources(fileName);
```

getClassLoader().getResources会加载到类[```ExtensionLoader```]所属模块以及该模块所依赖的模块下的资源。