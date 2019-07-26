//package zhongchiedu.compent;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.shiro.SecurityUtils;
//import org.apache.shiro.authc.UsernamePasswordToken;
//import org.apache.shiro.session.Session;
//import org.apache.shiro.subject.Subject;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
//
//import lombok.extern.slf4j.Slf4j;
//import zhongchiedu.common.utils.Contents;
//import zhongchiedu.general.pojo.User;
//import zhongchiedu.general.service.Impl.UserServiceImpl;
//
//@Component
//@Slf4j
//public class SessionInterceptor implements HandlerInterceptor {
//	
//	@Autowired
//	private UserServiceImpl userService;
//
//	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
//			throws Exception {
//		//HttpSession session = request.getSession();
//		log.info("session interceptor");
//		Subject currentUser = SecurityUtils.getSubject();
//		//判断用户是通过记住我功能自动登录，此时session失效
//		if(!currentUser.isAuthenticated()&&currentUser.isRemembered()){
//			log.info("记住我----同时session失效了");
//			System.out.println(currentUser.getPrincipal().toString());
//			try{
//				User user = this.userService.findUserByAccountName(currentUser.getPrincipal().toString());
//				//对密码进行验证
//				UsernamePasswordToken token = new UsernamePasswordToken(user.getAccountName(), user.getPassWord(),currentUser.isRemembered());
//				//将当前用户存入session
//				currentUser.login(token);
//				Session session = currentUser.getSession();
//				session.setAttribute(Contents.USER_SESSION, user);
//				return true;
//			}catch(Exception e){
//			//	response.sendRedirect(request.getContextPath()+"/login");
//				return false;
//			}
//		}
////		if(!currentUser.isAuthenticated()){
////            //自动登录失败,跳转到登录页面
////            response.sendRedirect(request.getContextPath()+"/login");
////            return false;
////        }
//		return true;
//		
//		
//	}
//
//	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
//			ModelAndView modelAndView) throws Exception {
//	}
//
//	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
//			throws Exception {
//		// 通过urlid查询父目录以及urlid所在的资源
//
//	}
//
//}
