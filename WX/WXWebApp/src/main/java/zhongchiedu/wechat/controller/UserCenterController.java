package zhongchiedu.wechat.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.Impl.UserServiceImpl;

@Controller
@RequestMapping("/wechat")
public class UserCenterController {


	@Autowired
	private UserServiceImpl userService;


	

	/**
	 * 解除绑定
	 * 
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/unbind")
	@ResponseBody
	public BasicDataResult out(String userId, HttpSession session) {
		try {
			
			User user = this.userService.findOneById(userId, User.class);
			if(Common.isEmpty(user)) {
				return new BasicDataResult().build(400, "解绑失败，请联系管理员！", null);
			}
			
			if(Common.isNotEmpty(userId)) {
				this.userService.updateUserAddOpenId(userId,null);
				session.removeAttribute(Contents.WECHAT_WEB_SESSION);
				return new BasicDataResult().build(200, "解除绑定成功！", null);
			}else {
				return new BasicDataResult().build(400, "解绑失败，请联系管理员！", null);
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			return new BasicDataResult().build(400, "解绑失败，请联系管理员！", null);
		}
		
	}

	
	

	@RequestMapping(value = "/checkPassword", method = RequestMethod.POST)
	@ResponseBody 
	public BasicDataResult checkPassword(@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "password", defaultValue = "") String password) {
		
		return this.userService.checkPassword(id, password);
		
	}

	
	@RequestMapping(value = "/editPassword", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult editPassword(@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "password2", defaultValue = "") String password2,HttpSession session) {
		session.removeAttribute(Contents.WECHAT_WEB_SESSION);
		return this.userService.editPassword(id, password2);
	}
	
	
	
	@RequestMapping(value = "/changePwd" ,method = RequestMethod.GET)
	public String toEditPasswordPage() {
		
		return "userCenter/changePwd";
	}
	
	
	
	
	
	
	
	
	

	
}



