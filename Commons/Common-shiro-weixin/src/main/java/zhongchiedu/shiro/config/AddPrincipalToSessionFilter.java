package zhongchiedu.shiro.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;

import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.Impl.UserServiceImpl;

public class AddPrincipalToSessionFilter extends OncePerRequestFilter {
	
	@Autowired
	private UserServiceImpl userService;

	@Override
	protected void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		
		 Subject subject = SecurityUtils.getSubject();
	        if (subject.isRemembered()) {
	        	User user = this.userService.findUserByAccountName(subject.getPrincipal().toString());
	            //将用户信息存入session
	        	//对密码进行验证
				UsernamePasswordToken token = new UsernamePasswordToken(user.getAccountName(), user.getPassWord(),subject.isRemembered());
				//将当前用户存入session
				subject.login(token);
				Session session = subject.getSession();
				session.setAttribute(Contents.WECHAT_WEB_SESSION, user);
	        }
	        chain.doFilter(request, response);
	}
}