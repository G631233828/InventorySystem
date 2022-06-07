package zhongchiedu.wechat.compent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.wx.config.WxMpProperties;

/**
 * 前台拦截器， 可以添加黑名单
 * 
 * @author fliay
 *
 */
@Component
public class WeiWebHandlerInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(WeiWebHandlerInterceptor.class);

//	@Autowired
//	private RoleService roleService;

	@Autowired
	private WxMpProperties wxMpProperties;

	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;
	@Autowired
	private WxMpService wxMpService;
	@Autowired
	private UserService userService;

	@SuppressWarnings("unchecked")
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		HttpSession session = request.getSession();
		log.info("请求的uri：" + request.getRequestURI() + ",请求的IP：" + Common.getHostIp());
		User suser = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);

		if (Common.isNotEmpty(suser)) {
			UsernamePasswordToken token = new UsernamePasswordToken(suser.getAccountName(), suser.getPassWord());
			Subject subject = SecurityUtils.getSubject();
			subject.login(token);
		} else {
			// 获取微信端code
			String code = request.getParameter("code");
			if(code!=null) {
				WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
				WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
//					WxMpOAuth2AccessToken oauth2getAccessToken = wxMpService.oauth2getAccessToken(code);
				String openId = accessToken.getOpenId();
				// 通过openId判断用户是否绑定账号
				User user = this.userService.findUserByOpenId(openId);
				if (user != null) {
					UsernamePasswordToken token = new UsernamePasswordToken(user.getAccountName(), user.getPassWord());
					Subject subject = SecurityUtils.getSubject();
					subject.login(token);
				}
			}
			

		}

//		//修改权限之后需要去刷新用户的权限
//		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
//		if(Common.isNotEmpty(user)){
//			//获取session中所属角色的id
//			Role sessionRole = user.getRole();
//			//通过roleID 查找数据库中的role
//			Role role = this.roleService.findOneById(sessionRole.getId(), Role.class);
//			if(sessionRole.getVersion() == role.getVersion()){
//				return true;
//			}else{
//				SecurityUtils.getSubject().logout();
//			}
//		}
//		
		return true;

	}

	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
	}

	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {

	}

}
