package zhongchiedu.wechat.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.aliyun.dingtalk.service.DingTalkUserService;
import com.dingtalk.api.response.OapiV2UserGetResponse.UserGetResponse;

import zhongchiedu.common.utils.Common;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;

@Controller
public class DingdingLoginController {



	@Autowired
	private UserService userService;


	@Autowired
    private DingTalkUserService dingTalkUserService;
	
    /**
     * 根据免登授权码, 获取登录用户身份
     *
     * @param authCode 免登授权码
     * @return
     */
    @GetMapping("/ddlogin")
    public String login(String authCode,Model model) {
    	if(Common.isEmpty(authCode)) {
    		return "dingdinglogin"; 
    	}else {
    			String msg = "";
    			UserGetResponse userInfo = dingTalkUserService.getUserInfo(authCode);
    			//通过手机号获取账号密码
    			if(Common.isNotEmpty(userInfo.getMobile())) {
    			User user = this.userService.findUserByAccountName(userInfo.getMobile());
    				if(Common.isNotEmpty(user)) {

        				UsernamePasswordToken token = new UsernamePasswordToken(user.getAccountName(), user.getPassWord(),true);
        				// token.setRememberMe(rememberMe);
        				Subject subject = SecurityUtils.getSubject();// 获得主体
        				try {
        					subject.login(token);
        					if (subject.isAuthenticated()) {
        						return "redirect:wechat/index";
        					} else {
        						msg = "登录失败";
        					}
        				} catch (IncorrectCredentialsException e) {
        					msg = "登录密码错误!";
        				} catch (ExcessiveAttemptsException e) {
        					msg = "登录失败次数过多!";
        				} catch (LockedAccountException e) {
        					msg = "帐号已被锁定!" ;
        				} catch (DisabledAccountException e) {
        					msg = "帐号已被禁用,请与管理员联系!" ;
        				} catch (ExpiredCredentialsException e) {
        					msg = "帐号已过期!";
        				} catch (UnknownAccountException e) {
        					msg = "帐号不存在!";
        				} catch (UnauthorizedException e) {
        					msg = "您没有得到相应的授权！" + e.getMessage();
        				} finally {
        					model.addAttribute("msg", msg);
        				}
        				return "login";
        			
    				
    				}
    			
    				
    			}
    			
    	
    		
    	}
		return "login";
    	
    	
    	
    	
    	
       
//        return ServiceResult.getSuccessResult(dingTalkUserService.getUserInfo(authCode));

    }
	


}
