package zhongchiedu.wechat.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.common.util.crypto.SHA1;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechat")
public class WeiLoginController {

	@Autowired
	private WxMpService wxMpService;
//	
//	@Autowired
//	private WxMpUserService wxMpUserService;

	@Autowired
	private UserService userService;

	@Autowired
	private WxMpProperties wxMpProperties;

	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;

	/**
	 * 登陆授权
	 * 
	 * @param request
	 * @return
	 * @throws WxErrorException
	 */
	@RequestMapping("/weChatAuth")
	public String weChatAuth(HttpSession session, HttpServletRequest request, RedirectAttributes attr)
			throws WxErrorException {
		User suser = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		if (Common.isNotEmpty(suser)) {
			UsernamePasswordToken token = new UsernamePasswordToken(suser.getAccountName(), suser.getPassWord());
			Subject subject = SecurityUtils.getSubject();
			subject.login(token);
			return "redirect:index";
		}

		// 获取微信端code
		String code = request.getParameter("code");
		// 判断code是否为空，如果为空的话需要通过微信进行重定向
		if (Common.isEmpty(code)) {
			String redirect_uri = wxMpProperties.getServerUrl() + "/WXWebApp/wechat/weChatAuth";
//				return new ModelAndView(new RedirectView("https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
//						+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
//						+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect"));
			return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
					+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
					+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
		}
		// 通过code获取微信相关信息

		
		WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
		WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
//		WxMpOAuth2AccessToken oauth2getAccessToken = wxMpService.oauth2getAccessToken(code);
		String openId = accessToken.getOpenId();
		// 通过openId判断用户是否绑定账号
		User user = this.userService.findUserByOpenId(openId);

		if (Common.isEmpty(user)) {
			attr.addFlashAttribute("openId", openId);
			// 获取数据为空，跳转登陆绑定界面
			return "redirect:login";
		}
		UsernamePasswordToken token = new UsernamePasswordToken(user.getAccountName(), user.getPassWord());
		
		// 登陆成功
//		UsernamePasswordToken token = new UsernamePasswordToken("admin", "111111");
		Subject subject = SecurityUtils.getSubject();
		subject.login(token);
//		session.setAttribute(Contents.WECHAT_WEB_SESSION, user);
		return "redirect:index";
		// 跳转登陆成功界面

//		//通过用户openId获取用户信息
//		WxMpUser userInfo = wxMpService.getUserService().userInfo(oauth2getAccessToken.getOpenId());

//	System.out.println(wxMpService.getAccessToken());
//	WxMpOAuth2AccessToken oauth2getAccessToken = wxMpService.oauth2getAccessToken("");
//	
//	WxMpUser userInfo = wxMpUserService.userInfo(oauth2getAccessToken.getOpenId());

	}

	@PostMapping(value = "tologin")
	public String tologin(HttpSession session, String username, String password, String openId,
			RedirectAttributes attr) {
		if (Common.isEmpty(openId)) {
			// 跳转授权界面
			attr.addFlashAttribute("error", "未能获取到用户openId");
			return "redirect:weChatAuth";
		}
		if (Common.isEmpty(username)) {
			// 跳转授权界面
			attr.addFlashAttribute("error", "用户名不能为空");
			return "redirect:login";
		}
		if (Common.isEmpty(password)) {
			// 跳转授权界面
			attr.addFlashAttribute("error", "密码不能为空");
			return "redirect:login";
		}
		// User user = this.userService.findUserByUserNameAndPassword(username,
		// password);
		UsernamePasswordToken token = new UsernamePasswordToken(username, password);
		String msg = "";
		Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(token);
			User user = this.userService.findUserByAccountName(username);
			System.out.println(openId);
			//更新openId
			if(Common.isNotEmpty(openId)) {
				System.out.println("更新openId");
				this.userService.updateUserAddOpenId(user.getId(), openId);
			}
			
			if (subject.isAuthenticated()) {
				return "redirect:index";
			} else {
				msg = "登录失败";
			}
		} catch (IncorrectCredentialsException e) {
			msg = "登录密码错误!";
		} catch (ExcessiveAttemptsException e) {
			msg = "登录失败次数过多!";
		} catch (LockedAccountException e) {
			msg = "帐号已被锁定!";
		} catch (DisabledAccountException e) {
			msg = "帐号已被禁用,请与管理员联系!";
		} catch (ExpiredCredentialsException e) {
			msg = "帐号已过期!";
		} catch (UnknownAccountException e) {
			msg = "帐号不存在!";
		} catch (UnauthorizedException e) {
			msg = "您没有得到相应的授权！" + e.getMessage();
		} finally {
			attr.addFlashAttribute("error", msg);
		}

		return "redirect:login";
		// 根据用户id绑定openId
//		this.userService.updateUserAddOpenId(user.getId(), openId);
//		return "redirect:index";
	}

	@Value("${wx.mp.server-url}")
	private String server_url;

	/**
	 * 跳转首页
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/index")
	public String index(Model model) {

		return "index";
	}

	/**
	 * 跳转登陆界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/login")
	public String login(@ModelAttribute("openId") String openId, @ModelAttribute("error") String error, Model model) {
		model.addAttribute("openId", openId);
		model.addAttribute("error", error);
		return "login";
	}

	public static void main(String[] args) throws WxErrorException {
		WxMpService wx = new WxMpServiceImpl();
		String token = wx.getAccessToken();
		System.out.println(token);
	}

	@ResponseBody
	@PostMapping("/getWxConfig")
	public Map getWxConfig() {
		Map map = new HashMap();
		// 生成签名 随机字符串等
		try {
			String noncestr = UUID.randomUUID().toString().replace("-", "").substring(0, 16);// 随机字符串
			String timestamp = String.valueOf(System.currentTimeMillis() / 1000);// 时间戳
			// String accessToken = this.wxMpService.getAccessToken();
			String jsapiTicket = wxMpService.getJsapiTicket();
			// System.out.println(accessToken);
			System.out.println(jsapiTicket);
			String url = server_url + "/WXWebApp/wechat/stock";
			// 5、将参数排序并拼接字符串
			String str = "jsapi_ticket=" + jsapiTicket + "&noncestr=" + noncestr + "&timestamp=" + timestamp + "&url="
					+ url;
			System.out.println("str" + str);
			String signature = SHA1.gen(str);
			System.out.println("signature" + signature);
			map.put("noncestr", noncestr);
			map.put("timestamp", timestamp);
			map.put("signature", signature);
			map.put("appId", appid);

		} catch (WxErrorException e) {
			e.printStackTrace();
		}
		return map;

	}

}
