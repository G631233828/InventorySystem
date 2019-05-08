package zhongchiedu.compent;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.Role;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.RoleService;

@Component
public class LoginHandlerInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(LoginHandlerInterceptor.class);
	@Autowired
	private RoleService roleService;
	
	
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		HttpSession session = request.getSession();
		String urlid = request.getParameter("urlid");
		if(Common.isNotEmpty(urlid)){
			session.setAttribute(Contents.MENU_ID, urlid);
		}
		
		//修改权限之后需要去刷新用户的权限
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		if(Common.isNotEmpty(user)){
			//获取session中所属角色的id
			Role sessionRole = user.getRole();
			//通过roleID 查找数据库中的role
			Role role = this.roleService.findOneById(sessionRole.getId(), Role.class);
			if(sessionRole.getVersion() == role.getVersion()){
				return true;
			}else{
				SecurityUtils.getSubject().logout();
			}
			
			
		}
	
	
		
		return true;


	}

	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
	}

	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// 通过urlid查询父目录以及urlid所在的资源
		
	}

}
