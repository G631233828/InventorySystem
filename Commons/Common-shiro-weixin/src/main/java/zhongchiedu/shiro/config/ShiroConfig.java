package zhongchiedu.shiro.config;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.DelegatingFilterProxy;

import lombok.extern.slf4j.Slf4j;

/**
 * 配置shiro
 * 
 * @author fliay
 *
 */
@Configuration
@Slf4j
public class ShiroConfig {


	@Bean
	public MongoDBRealm mongoDBRealm() {
		MongoDBRealm mongoDBRealm = new MongoDBRealm();
		return mongoDBRealm;
	}

	/**
	 * 密码匹配凭证管理器
	 * 
	 * @return
	 */
	@Bean
	public HashedCredentialsMatcher hashedCredentialsMatcher() {
		log.info("hashedCredentialsMatcher()");
		HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher();
		hashedCredentialsMatcher.setHashAlgorithmName("MD5");// 散列算法:这里使用MD5算法;
		hashedCredentialsMatcher.setHashIterations(1024);// 散列的次数，比如散列两次，相当于
															// md5(md5(""));
		return hashedCredentialsMatcher;
	}

	/**
	 * 开启shiro aop注解支持. 使用代理方式;所以需要开启代码支持; Controller才能使用@RequiresPermissions
	 * 
	 * @param securityManager
	 * @return
	 */
	@Bean
	public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
			@Qualifier("securityManager") SecurityManager securityManager) {
		log.info("authorizationAttributeSourceAdvisor()");
		AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
		authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
		return authorizationAttributeSourceAdvisor;
	}

	/**
	 * shiro缓存管理器; 需要注入对应的其它的实体类中： 1、安全管理器：securityManager
	 * 可见securityManager是整个shiro的核心；
	 * 
	 * @return
	 */

	@Bean
	public EhCacheManager ehCacheManager() {
		log.info("ShiroConfiguration.getEhCacheManager()");
		EhCacheManager cacheManager = new EhCacheManager();
		cacheManager.setCacheManagerConfigFile("classpath:ehcache-shiro.xml");
		return cacheManager;

	}

	@Bean
	public SecurityManager securityManager() {
		DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
		securityManager.setRealm(mongoDBRealm());
		// 注入缓存管理器;
		securityManager.setCacheManager(ehCacheManager());// 这个如果执行多次，也是同样的一个对象;

		securityManager.setRememberMeManager(rememberMeManager());

		return securityManager;
	}

	/**
	 * Cookie对象，会话Cooke模板，默认为：JSESSIONID 问题：与servlet容器名冲突，重新定义为sid或rememberMe
	 * 
	 * @return
	 */
	@Bean
	public SimpleCookie rememberMeCookie() {
		SimpleCookie simpleCookie = new SimpleCookie("rememberMe");// 参数是cookie的名称，对于前端checkbox中name=rememberMe
		// setCooke的httponly 属性如果为true 会增加对xss防护的安全系数
		// 设为true之后只能通过http访问，无法通过javascript访问
		// 防止xss读取cookie
		simpleCookie.setHttpOnly(true);
		simpleCookie.setPath("/");
		simpleCookie.setMaxAge(-1);
		return simpleCookie;
	}

	/**
	 * Cookie管理对象；记住我功能 rememberMe管理器
	 * 
	 * @return
	 */
	@Bean
	public CookieRememberMeManager rememberMeManager() {
		CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
		cookieRememberMeManager.setCookie(rememberMeCookie());
		// rememberMe cookie加密的密钥，建议每个项目都不一样，默认AES算法密钥长度（128， 256 ，512位）
		cookieRememberMeManager.setCipherKey(Base64.decode("4AvVhmFLUs0KTA3Kprseaf=="));
		return cookieRememberMeManager;
	}

	/**
	 * FormAuthenticationFilter 过滤器 过滤记住我
	 * 
	 * @return
	 */
	@Bean
	public FormAuthenticationFilter formAuthenticationFilter() {
		FormAuthenticationFilter formfilter = new FormAuthenticationFilter();
		formfilter.setRememberMeParam("rememberMe");
		return formfilter;
	}

	@Bean
	public FilterRegistrationBean delegatingFilterProxy() {
		 FilterRegistrationBean registration = new FilterRegistrationBean();
	        registration.setFilter(new DelegatingFilterProxy("shiroFilter"));
	        //该值缺省为false，表示生命周期由SpringApplicationContext管理，设置为true则表示由ServletContainer管理
	        registration.addInitParameter("targetFilterLifecycle", "true");
	        registration.setEnabled(true);
	        registration.addUrlPatterns("/*");
	        return registration;
	}
	@Bean(name = "shiroFilter")
	public ShiroFilterFactoryBean ShiroFilter(SecurityManager securityManager) {
		ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
		shiroFilterFactoryBean.setSecurityManager(securityManager);
		// 拦截器.
		Map<String, Filter> filters = shiroFilterFactoryBean.getFilters();//获取filters
        filters.put("addPrincipal",addPrincipalToSessionFilter());//rememberMe存session过滤器
		
		Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();
		// 配置不会被拦截的链接 顺序判断
		filterChainDefinitionMap.put("/static/**", "anon");
		filterChainDefinitionMap.put("/assets/**", "anon");
		filterChainDefinitionMap.put("/wechat/tologin", "anon");
		filterChainDefinitionMap.put("/wechat/portal/**", "anon");
		filterChainDefinitionMap.put("/wechat/preStock/**", "anon");
		filterChainDefinitionMap.put("/wechat/cargoFromStorage/**", "anon");
		filterChainDefinitionMap.put("/wechat/projectPickup", "anon");
		filterChainDefinitionMap.put("/wechat/index", "user,addPrincipal");
		filterChainDefinitionMap.put("/wechat/weChatAuth", "anon");
		filterChainDefinitionMap.put("/wechat/preStockToStock/**", "anon");
		filterChainDefinitionMap.put("/wechat/pickUpApplicationToStock/**", "anon");
		filterChainDefinitionMap.put("/wechat/login", "anon");
		// 配置退出 过滤器,其中的具体的退出代码Shiro已经替我们实现了
		// filterChainDefinitionMap.put("/loginout", "loginout");
		// <!-- 过滤链定义，从上向下顺序执行，一般将/**放在最为下边 -->:这是一个坑呢，一不小心代码就不好使了;
		// <!-- authc:所有url都必须认证通过才可以访问; anon:所有url都都可以匿名访问-->
		// authc 表示所有资源需要认证才能进行访问，user表示配置记住我或认证通过可以访问的地址
		filterChainDefinitionMap.put("/**", "user");
		// 如果不设置默认会自动寻找Web工程根目录下的"/login.jsp"页面
		// 登录成功后要跳转的链接
		// shiroFilterFactoryBean.setSuccessUrl("/website/index");
		shiroFilterFactoryBean.setLoginUrl("/wechat/weChatAuth");
		// // 登录成功后要跳转的链接
		shiroFilterFactoryBean.setSuccessUrl("/wechat/toindex");
		// 未授权界面;
		shiroFilterFactoryBean.setUnauthorizedUrl("/403");
		shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
		
		
		
		return shiroFilterFactoryBean;

	}
	
	@Bean
	public AddPrincipalToSessionFilter addPrincipalToSessionFilter(){
	    return  new AddPrincipalToSessionFilter();
	}
	   

}
