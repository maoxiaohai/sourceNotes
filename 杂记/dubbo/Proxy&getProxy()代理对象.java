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