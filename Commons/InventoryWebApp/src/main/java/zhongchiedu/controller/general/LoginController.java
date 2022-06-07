package zhongchiedu.controller.general;


import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.PickUpApplication;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.Impl.PickUpApplicationServiceImpl;
import zhongchiedu.inventory.service.Impl.PreStockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
public class LoginController {
	private static final Logger log = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	private UserService userService;

	@RequestMapping("/tologin")
	@SystemControllerLog(description = "用户申请登陆")
	public String login(User user,boolean rememberMe, HttpServletRequest request, Map<String, Object> map, HttpSession session,Model model)
			throws Exception {
		if(Common.isEmpty(user.getAccountName())||Common.isEmpty(user.getPassWord())){
			return "login";
		}
		String msg = "";
			/*
			 * boolean rememberMe = false; if (remember != "") { rememberMe =
			 * true; }
			 */
			String accountName = user.getAccountName();
			String password = user.getPassWord();
			if (accountName != "" && password != "") {
				UsernamePasswordToken token = new UsernamePasswordToken(accountName, password,rememberMe);
				// token.setRememberMe(rememberMe);
				Subject subject = SecurityUtils.getSubject();// 获得主体
				try {
					subject.login(token);
					if (subject.isAuthenticated()) {
						return "redirect:/toindex";
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

		return "redirect:/toindex";
	}

	/**
	 * 登出
	 * 
	 * @return
	 */
	@RequestMapping(value = "/loginOut")
	@SystemControllerLog(description = "用户退出")
	public String loginOut(HttpSession session,HttpServletResponse resp) {
		Subject subject = SecurityUtils.getSubject();// 获得主体
		session.removeAttribute(Contents.USER_SESSION);//删除cookie
		Cookie co = new Cookie("accountName", "");
		co.setMaxAge(0);// 设置立即过期
		co.setPath("/");// 根目录，整个网站有效
		resp.addCookie(co);
		subject.logout();
		return "login";
	}

	
	private @Autowired StockServiceImpl stockService;
	private @Autowired StockStatisticsServiceImpl stockStatisticsService; 
	private @Autowired PreStockServiceImpl preStockServiceImpl;
	private @Autowired PickUpApplicationServiceImpl pickUpApplicationService;
	
	
	@RequestMapping(value="/toindex")
	@SystemControllerLog(description = "用户登陆成功")
	public String toindex(Model model){
		//今日出库
		List<StockStatistics> out = this.stockStatisticsService.findAllByDate(Common.fromDateYMD(), false);
		//今日入库
		List<StockStatistics> in = this.stockStatisticsService.findAllByDate(Common.fromDateYMD(), true);
		//低库存
		List<Stock> low = this.stockService.findLowStock(3);
		//预入库
		List<PreStock> findAllPreStockByStatus = this.preStockServiceImpl.findAllPreStockByStatus(false, 1);
		//预出库
		List<PickUpApplication> findAllPickUpApplicationByStatus = this.pickUpApplicationService.findAllPickUpApplicationByStatus(false, 1);
		
		model.addAttribute("out", out);
		model.addAttribute("in", in);
		model.addAttribute("low", low);
		model.addAttribute("preStocks", findAllPreStockByStatus);
		model.addAttribute("pickUpApplication", findAllPickUpApplicationByStatus);
		return "index";
	}
	

}
