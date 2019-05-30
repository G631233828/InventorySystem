package zhongchiedu.inventory.pojo;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;


/**
 * 库存统计
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StockStatistics extends GeneralBean<StockStatistics> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8545694298499828935L;
	
	
	@DBRef
	private Stock stock; //绑定库存商品
	private Date storageTime;//入库时间
	private Date DepotTime;//出库时间
	private long num;//出库、入库数量
	private long newNum;//当前库存
	@DBRef
	private Companys company;//绑定企业
	@DBRef
	private User user;//操作人
	private boolean revoke; //是否侧撤销
	private boolean InOrOut;//入库还是出库
	
	

	
	org.springframework.dao.DuplicateKeyException: insertDocument :: caused by :: 11000 E11000 duplicate key error index: InventorySystem.stockStatistics.$_id_  dup key: { : ObjectId('5cef9f65a60d659a24d48638') }; nested exception is com.mongodb.MongoWriteException: insertDocument :: caused by :: 11000 E11000 duplicate key error index: InventorySystem.stockStatistics.$_id_  dup key: { : ObjectId('5cef9f65a60d659a24d48638') }
	at org.springframework.data.mongodb.core.MongoExceptionTranslator.translateExceptionIfPossible(MongoExceptionTranslator.java:98)
	at org.springframework.data.mongodb.core.MongoTemplate.potentiallyConvertRuntimeException(MongoTemplate.java:2592)
	at org.springframework.data.mongodb.core.MongoTemplate.execute(MongoTemplate.java:526)
	at org.springframework.data.mongodb.core.MongoTemplate.insertDocument(MongoTemplate.java:1270)
	at org.springframework.data.mongodb.core.MongoTemplate.doInsert(MongoTemplate.java:1051)
	at org.springframework.data.mongodb.core.MongoTemplate.insert(MongoTemplate.java:988)
	at org.springframework.data.mongodb.core.MongoTemplate.insert(MongoTemplate.java:974)
	at zhongchiedu.framework.dao.GeneralDaoImpl.insert(GeneralDaoImpl.java:42)
	at zhongchiedu.framework.dao.GeneralDaoImpl$$FastClassBySpringCGLIB$$1f2547d8.invoke(<generated>)
	at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:204)
	at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:746)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:139)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:185)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:688)
	at zhongchiedu.framework.dao.GeneralDaoImpl$$EnhancerBySpringCGLIB$$6ea62dae.insert(<generated>)
	at zhongchiedu.framework.service.GeneralServiceImpl.insert(GeneralServiceImpl.java:49)
	at zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl.inOrOutstockStatistics(StockStatisticsServiceImpl.java:69)
	at zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl$$FastClassBySpringCGLIB$$f99caabd.invoke(<generated>)
	at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:204)
	at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:746)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:139)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:185)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:688)
	at zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl$$EnhancerBySpringCGLIB$$d4dacd7d.inOrOutstockStatistics(<generated>)
	at zhongchiedu.test.StockStatisticsTest.execute(StockStatisticsTest.java:53)
	at zhongchiedu.test.StockStatisticsTest$1.run(StockStatisticsTest.java:43)
	at java.lang.Thread.run(Thread.java:748)
	at zhongchiedu.test.StockStatisticsTest.test(StockStatisticsTest.java:45)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.springframework.test.context.junit4.statements.RunBeforeTestExecutionCallbacks.evaluate(RunBeforeTestExecutionCallbacks.java:73)
	at org.springframework.test.context.junit4.statements.RunAfterTestExecutionCallbacks.evaluate(RunAfterTestExecutionCallbacks.java:83)
	at org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks.evaluate(RunBeforeTestMethodCallbacks.java:75)
	at org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks.evaluate(RunAfterTestMethodCallbacks.java:86)
	at org.springframework.test.context.junit4.statements.SpringRepeat.evaluate(SpringRepeat.java:84)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.springframework.test.context.junit4.SpringJUnit4ClassRunner.runChild(SpringJUnit4ClassRunner.java:251)
	at org.springframework.test.context.junit4.SpringJUnit4ClassRunner.runChild(SpringJUnit4ClassRunner.java:97)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks.evaluate(RunBeforeTestClassCallbacks.java:61)
	at org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks.evaluate(RunAfterTestClassCallbacks.java:70)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.springframework.test.context.junit4.SpringJUnit4ClassRunner.run(SpringJUnit4ClassRunner.java:190)
	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:86)
	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:459)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:678)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:382)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:192)
Caused by: com.mongodb.MongoWriteException: insertDocument :: caused by :: 11000 E11000 duplicate key error index: InventorySystem.stockStatistics.$_id_  dup key: { : ObjectId('5cef9f65a60d659a24d48638') }
	at com.mongodb.MongoCollectionImpl.executeSingleWriteRequest(MongoCollectionImpl.java:1033)
	at com.mongodb.MongoCollectionImpl.executeInsertOne(MongoCollectionImpl.java:513)
	at com.mongodb.MongoCollectionImpl.insertOne(MongoCollectionImpl.java:493)
	at com.mongodb.MongoCollectionImpl.insertOne(MongoCollectionImpl.java:487)
	at org.springframework.data.mongodb.core.MongoTemplate$8.doInCollection(MongoTemplate.java:1276)
	at org.springframework.data.mongodb.core.MongoTemplate.execute(MongoTemplate.java:524)
	... 58 more


	
	

}
