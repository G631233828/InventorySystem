package zhongchiedu.wechat.config;



import java.io.File;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionTrackingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
//import zhongchiedu.wechat.compent.WeChatFilter;
import zhongchiedu.wechat.compent.WeiWebHandlerInterceptor;


@Configuration
public class MVCConfig extends WebMvcConfigurerAdapter {

	@Value("${upload-imgpath}")
	private String imgpath;
////	@Value("${upload.kinderitor}")
////	private String kinderitor;
	@Value("${upload-dir}")
	private String dir;
//	@Value("${upload.ueditor}")
//	private String ueditor;
//	@Value("${upload.ueditordir}")
//	private String ueditordir;
//	
//	upload-dir=d:/zc/
//	upload-imgpath=/upload/image/
	 @Override
	    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//	    	registry.addResourceHandler("/img/**").addResourceLocations("file:"+filepath+"/"); 
//	    	registry.addResourceHandler(savePath+"/**").addResourceLocations("file:"+savePath+"/");
//	    	registry.addResourceHandler(kinderitor+"/**").addResourceLocations("file:"+dir+kinderitor+"/");
	    	registry.addResourceHandler(imgpath+"/**").addResourceLocations("file:"+dir+imgpath+File.separator);
//	    	registry.addResourceHandler(imgpath+"/**").addResourceLocations("file:"+dir+imgpath+"/");
//	    	registry.addResourceHandler(ueditor+"/**").addResourceLocations("file:"+dir+ueditor+"/");
//	    	registry.addResourceHandler(ueditordir+"/**").addResourceLocations("file:"+ueditordir+"/");
		    	super.addResourceHandlers(registry);
		    }
			  

	

	@Bean
	public WeiWebHandlerInterceptor weiWebHandlerInterceptor() {
		return new WeiWebHandlerInterceptor();
	}
	
//	@Bean
//	public WeChatFilter weChatFilter() {
//		return new WeChatFilter();
//	}

	/**
	 * shiro 界面整合 thymeleaf
	 * @return
	 */
	@Bean
    public ShiroDialect shiroDialect() {
        return new ShiroDialect();
    }



	// 配置多个访问地址自动转发
	@Bean
	public WebMvcConfigurerAdapter webMvcConfigurerAdapter() {
		WebMvcConfigurerAdapter adapter = new WebMvcConfigurerAdapter() {
			@Override
			public void addViewControllers(ViewControllerRegistry registry) {
				registry.addViewController("/wechat").setViewName("/wechat/weChatAuth");
				registry.addViewController("/wechatrp").setViewName("/wechatrp/repairlist");
			}


			//注册拦截器
			//不要使用new xxx否则拦截器中会注入失败
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				//registry.addInterceptor(sessionInterceptor()).addPathPatterns("/**").excludePathPatterns("/index.html","/user/login");
				 registry.addInterceptor(weiWebHandlerInterceptor()).addPathPatterns("/wechat").excludePathPatterns("/wechat/weChatAuth","/wechat/cargoFromStorage/**","/wechat/batchOut/**");
				// registry.addInterceptor(weChatFilter()).addPathPatterns("/wechatrp"); // 仅拦截需要强制微信访问的接口
			}
			
		};
		return adapter;
	}
	
	
	
	/**
	 * 解决静态资源中出现了jsessionid的问题
	 * @return
	 */
	@Bean
	public ServletContextInitializer servletContextInitializer1() {
	    return new ServletContextInitializer() {
	        @Override
	        public void onStartup(ServletContext servletContext) throws ServletException {
	            servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE) );
	        }
	    };
	}
	
	
	

	
	
	
	
	
	
	
	
	
	
}
	
