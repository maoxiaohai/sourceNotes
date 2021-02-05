### 1. JDBC查询数据

   ```java
//Step 1:加载驱动
Class.forName("com.mysql.jdbc.Driver");
String url = "jdbc:mysql://127.0.0.1:3306/school2?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC";
//Step 2:创建连接
Connection  con= DriverManager.getConnection(url, "root","123456" );
String sql ="select subjectName, emial FROM subject2";
//Step 3:创建Statement
PreparedStatement  stmt =connection.createStatement();
//Step 4:执行sql并获取查询结果
ResultSet  rs=stmt.executeQuery(sql);
while(rs.next()) {
    String a=rs.getString(1);
    int b = rs.getInt(2);
    System.out.println(a+"\t"+b);
  }
   ```

### 2. Mybatis查询数据

```java
Step 1:读取配置文件
Step 2:创建SqlSessionFactoryBuilder对象
Step 3:通过SqlSessionFactoryBuilder 对象创建SqlSessionFactory
Step 4:通过SqlSessionFactory 创建SqlSession
Step 5:为Dao 接口生成代理类
Step 6:使用接口方法访问数据库
```

其中Step 1--->Step3,是相同的，可以作为公共部分提取出来。

如下所示：

```
 @BeforeEach
  void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/typehandler/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
      sqlSessionFactory.getConfiguration().getTypeHandlerRegistry().register(StringTrimmingTypeHandler.class);
    }
    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
        "org/apache/ibatis/submitted/typehandler/CreateDB.sql");
  }
```

### 2.1  SqlSessionFactoryBuilder——>>>SqlSessionFactory

SqlSessionFactoryBuilder的作用就是创建SqlSessionFactory [此处是否用到了建造者模式？]



![image-20210125165001767](C:\Users\maoxiaohai\AppData\Roaming\Typora\typora-user-images\image-20210125165001767.png)

最后执行到的build方法如下所示：

```java
public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
      XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        reader.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }
```

真正创建SqlSessionFactory 的build方法如下：

```java
 public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
 }
```

可以看出来DefaultSqlSessionFactory 是其默认实现。SqlSessionManager[SqlSessionFactoryBuilder中是没有SqlSessionManager实现的，SqlSessionManager有点像是包装类]

![image-20210125180352814](C:\Users\maoxiaohai\AppData\Roaming\Typora\typora-user-images\image-20210125180352814.png)

### 2.2 xml节点解析

由SqlSessionFactoryBuilder的build()方法可知，创建最终的SqlSessionFactory时传入了一个Configuration对象，这个configuration对象由上一级的build()方法传入，

```java
 XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
 return build(parser.parse());
```

其实，在这里就能看出来，这里就是创建了一个解析器，然后将解析后的configuration传给SqlSessionFactory对象的创建。

```java
// XMLConfigBuilder构造函数
public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    //XMLMapperEntityResolver实际是在定位dtd校验文件
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
 }
```

其中，XPathParser的构造函数如下：

```java
 public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    //构建dom
    this.document = createDocument(new InputSource(reader));
  }
```



parser.parse()是真正解析节点的地方

```
 public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    //mybatis的根节点是 /configuration by mao
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }
```

具体的节点解析为：

```java
private void parseConfiguration(XNode root) {
    try {
      // issue #117 read properties first
      propertiesElement(root.evalNode("properties"));
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      loadCustomLogImpl(settings);
      typeAliasesElement(root.evalNode("typeAliases"));
      pluginElement(root.evalNode("plugins"));
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      typeHandlerElement(root.evalNode("typeHandlers"));
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }
```

#### 1. 解析properties属性

```java
private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      // Step 1:获取properties节点中获取子节点
      Properties defaults = context.getChildrenAsProperties();
      //Step 2： 从resource或者url获取配置文件[resource和url只能有一个存在，不能同时指定]
      String resource = context.getStringAttribute("resource");
      String url = context.getStringAttribute("url");
        //url和resource不能同时指定
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      if (resource != null) {
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
       //Step3:赋值
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }
```

##### Step 1: 解析properties节点的子节点[property],如图所示：

![image-20210126194841017](C:\Users\maoxiaohai\AppData\Roaming\Typora\typora-user-images\image-20210126194841017.png)

此时properties节点的配置如下：

````java
   <!-- properties属性还得放在前面, 搞不清楚是啥校验的-->
  <properties resource="jdbc.properties">
    <property name="username" value="233333"/>
    <property name="password" value="13213213"/>
  </properties>
````

##### Step2：从resource或者url获取配置文件[resource和url只能有一个存在，不能同时指定]，读取配置文件中的数据,作为配置文件。

此处可以看到的是，最终的配置项的值是以resource或者url中获取到的为准。



![image-20210126201737234](C:\Users\maoxiaohai\AppData\Roaming\Typora\typora-user-images\image-20210126201737234.png)



##### Step 3:将读取的配置数据给到parser和configuration

#### 2. 解析settings节点

代码如下：

```java
  private Properties settingsAsProperties(XNode context) {
    if (context == null) {
      return new Properties();
    }
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      //检查metaConfig是否有key对应的setter方法[其实就是在检查是否含有key属性]
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }
```

#### resultMap节点解析

##### resultMap节点的dtd文件定义：

```xml-dtd
//resultMap节点
<!ELEMENT resultMap (constructor?,id*,result*,association*,collection*, discriminator?)>
<!ATTLIST resultMap
id CDATA #REQUIRED
type CDATA #REQUIRED
extends CDATA #IMPLIED
autoMapping (true|false) #IMPLIED
>
/*************************************constructor节点**********************************/
//constructor节点
<!ELEMENT constructor (idArg*,arg*)>
//idArg
<!ELEMENT idArg EMPTY>
<!ATTLIST idArg
javaType CDATA #IMPLIED
column CDATA #IMPLIED
jdbcType CDATA #IMPLIED
typeHandler CDATA #IMPLIED
select CDATA #IMPLIED
resultMap CDATA #IMPLIED
name CDATA #IMPLIED
columnPrefix CDATA #IMPLIED
>
//arg
<!ELEMENT arg EMPTY>
<!ATTLIST arg
javaType CDATA #IMPLIED
column CDATA #IMPLIED
jdbcType CDATA #IMPLIED
typeHandler CDATA #IMPLIED
select CDATA #IMPLIED
 CDATA #IMPLIED
name CDATA #IMPLIED
columnPrefix CDATA #IMPLIED
>
/******************************constructor节点*****************************************/
//id 节点
<!ELEMENT id EMPTY>     //改节点不能有值
<!ELEMENT id EMPTY>
<!ATTLIST id
property CDATA #IMPLIED
javaType CDATA #IMPLIED
column CDATA #IMPLIED
jdbcType CDATA #IMPLIED
typeHandler CDATA #IMPLIED
>

//result
<!ELEMENT result EMPTY>
<!ATTLIST result
property CDATA #IMPLIED
javaType CDATA #IMPLIED
column CDATA #IMPLIED
jdbcType CDATA #IMPLIED
typeHandler CDATA #IMPLIED
>

//association
<!ELEMENT association (constructor?,id*,result*,association*,collection*, discriminator?)>
<!ATTLIST association
property CDATA #REQUIRED
column CDATA #IMPLIED
javaType CDATA #IMPLIED
jdbcType CDATA #IMPLIED
select CDATA #IMPLIED
resultMap CDATA #IMPLIED
typeHandler CDATA #IMPLIED
notNullColumn CDATA #IMPLIED
columnPrefix CDATA #IMPLIED
resultSet CDATA #IMPLIED
foreignColumn CDATA #IMPLIED
autoMapping (true|false) #IMPLIED
fetchType (lazy|eager) #IMPLIED
>

//collection
<!ELEMENT collection (constructor?,id*,result*,association*,collection*, discriminator?)>
<!ATTLIST collection
property CDATA #REQUIRED
column CDATA #IMPLIED
javaType CDATA #IMPLIED
ofType CDATA #IMPLIED
jdbcType CDATA #IMPLIED
select CDATA #IMPLIED
resultMap CDATA #IMPLIED
typeHandler CDATA #IMPLIED
notNullColumn CDATA #IMPLIED
columnPrefix CDATA #IMPLIED
resultSet CDATA #IMPLIED
foreignColumn CDATA #IMPLIED
autoMapping (true|false) #IMPLIED
fetchType (lazy|eager) #IMPLIED
>

//discriminator
<!ELEMENT discriminator (case+)>
<!ATTLIST discriminator
column CDATA #IMPLIED
javaType CDATA #REQUIRED
jdbcType CDATA #IMPLIED
typeHandler CDATA #IMPLIED
>
//case
<!ELEMENT case (constructor?,id*,result*,association*,collection*, discriminator?)>
<!ATTLIST case
value CDATA #REQUIRED
resultMap CDATA #IMPLIED
resultType CDATA #IMPLIED
>

//以下这两个节点猜测是共用的。
<!ELEMENT property EMPTY>
<!ATTLIST property
name CDATA #REQUIRED
value CDATA #REQUIRED
>
<!ELEMENT typeAlias EMPTY>
<!ATTLIST typeAlias
alias CDATA #REQUIRED
type CDATA #REQUIRED
>
```

也就是说：

| 元素          | 说明      |
| :------------ | --------- |
| constructor   | 出现0-1次 |
| id            | 出现0-n次 |
| result        | 出现0-n次 |
| association   | 出现0-n次 |
| collection    | 出现0-n次 |
| discriminator | 出现0-1次 |







