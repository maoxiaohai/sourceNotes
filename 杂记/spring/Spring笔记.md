```java
1. FactoryBeanRegistrySupport--->factoryBeanObjectCache
```

FactoryBeanRegistrySupport中factoryBeanObjectCache定义如下：

```java
/** Cache of singleton objects created by FactoryBeans: FactoryBean name --> object */
被FactoryBeans创建的singleton objects缓存
private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<String, Object>(16);
```

factoryBeanObjectCache和singletonObjects还是不一样的。

```java
getSingleton(String name)
```

一般通过这个方法获取单例对象,最终是落实在

```java
DefaultSingletonBeanRegistry类中的singletonObjects(Map)中
```

其定义如下

```java
/** Cache of singleton objects: bean name --> bean instance */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);
```

目前来看两者区别在于：

```java
singletonObjects是单例缓存，而factoryBeanObjectCache也算得上是单例缓存，但是factoryBeanObjectCache是由FactoryBean创建的实例。 	
```

获取bean的流程：

```java
AbstractApplicationContext-->>>getBean(String name)
  =====》》》BeanFactory---->>>>getBean(String name)
      AbstractBeanFactory---->>>doGetBean(***)------->>>>>>getObjectForBeanInstance(...)
        ------>>>getCachedObjectForFactoryBean(..)----如果没有获取到值--->>getObjectFromFactoryBean(..)
```



#### AbstractBeanFactory ---->doGetBean（...）代码如下：

```java

/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * @param name the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 * (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @param typeCheckOnly whether the instance is obtained for a type check,
	 * not for actual use
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 */
	@SuppressWarnings("unchecked")
	protected <T> T doGetBean(
			final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
			throws BeansException {

		final String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		//先从singletonObjects中获取单例
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			if (logger.isDebugEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			//再次去获取bean
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}
		.......
	}

```



#### getObjectForBeanInstance代码

```
protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		if (BeanFactoryUtils.isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
		}

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.
	    //如果获取到的bean不是一个FactoryBean类型，则直接返回获取到的bean，否则。create a bean instance
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			return beanInstance;
		}

		Object object = null;
		if (mbd == null) {
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// Return bean instance from factory.
			//*******
			//将这个bean作为factory
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// Caches object obtained from FactoryBean if it is a singleton.
			if (mbd == null && containsBeanDefinition(beanName)) {
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}
```



#### getObjectFromFactoryBean代码如下：

```java
protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
		if (factory.isSingleton() && containsSingleton(beanName)) {
			synchronized (getSingletonMutex()) {
                //先从factoryBeanObjectCache缓存中获取对象，刚开始肯定是获取不到的。object=null
				Object object = this.factoryBeanObjectCache.get(beanName);
				if (object == null) {
                    //从FactoryBeanRegistrySupport中获取单例对象
					object = doGetObjectFromFactoryBean(factory, beanName);
					// Only post-process and store if not put there already during getObject() call above
					// (e.g. because of circular reference processing triggered by custom getBean calls)
					Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
					if (alreadyThere != null) {
						object = alreadyThere;
					}
					else {
						if (object != null && shouldPostProcess) {
							try {
								object = postProcessObjectFromFactoryBean(object, beanName);
							}
							catch (Throwable ex) {
								throw new BeanCreationException(beanName,
										"Post-processing of FactoryBean's singleton object failed", ex);
							}
						}
						if (containsSingleton(beanName)) {
							this.factoryBeanObjectCache.put(beanName, (object != null ? object : NULL_OBJECT));
						}
					}
				}
				return (object != NULL_OBJECT ? object : null);
			}
		}
		else {
			Object object = doGetObjectFromFactoryBean(factory, beanName);
			if (object != null && shouldPostProcess) {
				try {
					object = postProcessObjectFromFactoryBean(object, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
				}
			}
			return object;
		}
	}
```

#### FactoryBeanRegistrySupport doGetObjectFromFactoryBean方法

```
private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
			throws BeanCreationException {

		Object object;
		try {
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
								return factory.getObject();
							}
						}, acc);
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
			//从工厂中获取对象,注意这个factory实际上就是自己实现的FactoryBean
				object = factory.getObject();
			}
		}
		catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		// Do not accept a null value for a FactoryBean that's not fully
		// initialized yet: Many FactoryBeans just return null then.
		if (object == null && isSingletonCurrentlyInCreation(beanName)) {
			throw new BeanCurrentlyInCreationException(
					beanName, "FactoryBean which is currently in creation returned null from getObject");
		}
		return object;
	}

```



