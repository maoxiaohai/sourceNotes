### 1. 内部类：
内部类可分为：
 
1. 成员内部类
2. 局部内部类
3. 匿名内部类
4. 静态内部类

以上暂且不做具体分析，只是抛砖引玉。

java 新特性首当其冲的就是lambda。 
而lambda其实就是匿名内部类的语法糖(实际不仅仅是语法糖，后面会进行具体分析) 
### 2. lambda
1. 函数式接口：其实就是指指包含一个函数的接口，可以使用```@FunctionalInterface```注解进行检查被修饰的接口是否符合函数式接口规范。
2. 其实lambda只能简化掉匿名内部类的接口名和参数传递，而且这依赖于javac的类型推断，如果推断失败就需要表明具体类型。
3. 参考文章链接：https://github.com/CarpenterLee/JavaLambdaInternals/blob/master/1-Lambda%20and%20Anonymous%20Classes(I).md

### 3. lambda内部实现原理
实际上lambda并不仅仅是匿名内部类的语法糖，因为在jvm层次上，内部类和lambda的实现完全不一样。lambda是通过invokedynamic指令实现的，而匿名内部类是通过创建一个新的类实现的，具体情况不是很清楚，有时间看下java 字节码相关的知识。

参考文章链接：https://github.com/CarpenterLee/JavaLambdaInternals/blob/master/2-Lambda%20and%20Anonymous%20Classes(II).md


### 4. Stream
流操作，实际跟流水线操作有点类似，流水线上是一个工序做完之后，紧接着进行下一个工序直至最后全部结束。
流操作包括中间操作和结束操作，只有结束操作才会触发中间操作，这就是经常说中间操作是懒执行的原因。

流操作的输出顺序
```
    Stream.of("d2", "a2", "b1", "b3", "c")
            .filter(s -> {
              System.out.println("filter: " + s);
              return true;             ---------------->返回值决定是否进行下一个操作
            })
            .forEach(s -> System.out.println("forEach: " + s));

            //
            // filter: d2
            // forEach: d2
            // filter: a2
            // forEach: a2
            // filter: b1
            // forEach: b1
            // filter: b3
            // forEach: b3
            // filter: c
            // forEach: c
```
输出结束是不是很诧异，其实就是这样的，流是随着链条垂直操作的。
再具体的操作看参考文章吧，不想写了
https://www.exception.site/java8/java8-stream-tutorial



