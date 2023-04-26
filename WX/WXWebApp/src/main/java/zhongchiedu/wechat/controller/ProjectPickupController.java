package zhongchiedu.wechat.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.ProjectPickup;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.Impl.NewCustomerServiceImpl;
import zhongchiedu.inventory.service.ProjectPickupService;
import zhongchiedu.inventory.service.SignService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;
import zhongchiedu.wx.pojo.WXUserInfo;
import zhongchiedu.wx.service.WXUserInfoService;


/**
 * 施工人员取货出库
 * @author fliay
 *
 */
@Controller
@RequestMapping("/wechat")
public class ProjectPickupController {
	
	@Autowired
	private WxMpService wxMpService;

	@Autowired
	private ProjectPickupService projectPickupService;
	
	@Autowired
	private WXUserInfoService wXUserInfoService;


	@Autowired
	private WxMpProperties wxMpProperties;
	
	@Autowired
	private StockStatisticsService stockStatisticsService;
	
	@Autowired
	private SignService signService;

	@Autowired
	private StockService stockService;

	@Autowired
	private NewCustomerServiceImpl newCustomerService;
	
	/**
	 * 出库页面 （扫码进去）
	 * @throws WxErrorException 
	 */
	@GetMapping(value = "/cargoFromStorage/{stockId}")
	public String cargoFromStorage(HttpServletRequest request,Model model,@PathVariable String stockId)  {
		
		// 获取微信端code
				String code = request.getParameter("code");
				// 判断code是否为空，如果为空的话需要通过微信进行重定向
				if (Common.isEmpty(code)) {
					String redirect_uri = wxMpProperties.getServerUrl() +  request.getRequestURI() ;
//						return new ModelAndView(new RedirectView("https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
//								+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
//								+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect"));
					return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
							+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
							+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
				}
		
		
		
		
		
		
		
		//获取库存商品信息
		Stock stock = this.stockService.findOneById(stockId, Stock.class);
		model.addAttribute("stock", stock);
		BasicDataResult dr=this.newCustomerService.ajaxgetCustomer("幼儿园");
		model.addAttribute("dr",dr);
		// 通过code获取微信相关信息
		WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
		WxOAuth2AccessToken accessToken;
		try {
			accessToken = oAuth2Service.getAccessToken(code);
			WxOAuth2UserInfo userInfo = oAuth2Service.getUserInfo(accessToken, null);
			model.addAttribute("userInfo", userInfo);
			WXUserInfo wXUserInfo = new WXUserInfo();
			wXUserInfo.setUserInfo(userInfo);
			wXUserInfo.setOpenId(userInfo.getOpenid());
			WXUserInfo getwXUserInfo = this.wXUserInfoService.saveOrUpdate(wXUserInfo);
			model.addAttribute("getwXUserInfo", getwXUserInfo);
		} catch (WxErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "general/out";
	}
	
	
	/**
	 * 出库页面 （扫码进去）
	 * @throws WxErrorException 
	 */
	@GetMapping(value = "/batchOut/{stockId}")
	public String batchOut(HttpServletRequest request,Model model,@PathVariable String stockId)  {

		
		
		// 获取微信端code
		String code = request.getParameter("code");
		// 判断code是否为空，如果为空的话需要通过微信进行重定向
		if (Common.isEmpty(code)) {
			String redirect_uri = wxMpProperties.getServerUrl() +  request.getRequestURI() ;
//				return new ModelAndView(new RedirectView("https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
//						+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
//						+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect"));
			return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
					+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
					+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
		}


		String path="general/batchOut";
		
		StockStatistics stock = this.stockStatisticsService.findOneById(stockId, StockStatistics.class);
		
		if(Common.isNotEmpty(stock.getMysign())) {
			//已经提交过
			Map<Object, Object> map = this.stockStatisticsService.stockStatisticsPickup(stock);
			model.addAttribute("map", map);
			if(Common.isNotEmpty(stock.getOpenId())) {
				WXUserInfo getwXUserInfo = this.wXUserInfoService.findUserByOpenId(stock.getOpenId());
				model.addAttribute("getwXUserInfo", getwXUserInfo);
			}
			
			List<StockStatistics> list = this.stockStatisticsService.findByoutboundOrder(stock.getOutboundOrder());
			model.addAttribute("list", list);
			
			path="general/batchOutSuccess";
			
		}else {
			List<StockStatistics> list = this.stockStatisticsService.findByoutboundOrder(stock.getOutboundOrder());
			model.addAttribute("list", list);
			model.addAttribute("stock", stock);

			// 通过code获取微信相关信息
			WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
			WxOAuth2AccessToken accessToken;
			try {
				accessToken = oAuth2Service.getAccessToken(code);
				WxOAuth2UserInfo userInfo = oAuth2Service.getUserInfo(accessToken, null);
				model.addAttribute("userInfo", userInfo);
				WXUserInfo wXUserInfo = new WXUserInfo();
				wXUserInfo.setUserInfo(userInfo);
				wXUserInfo.setOpenId(userInfo.getOpenid());
				WXUserInfo getwXUserInfo = this.wXUserInfoService.saveOrUpdate(wXUserInfo);
				model.addAttribute("getwXUserInfo", getwXUserInfo);
			} catch (WxErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		
		return path;
	}
	
	
	
	@PostMapping("/projectPickup")
	@SystemControllerLog(description = "库存取货")
	public String addprojectPickup(Model model,HttpServletRequest request, @ModelAttribute("projectPickup") ProjectPickup projectPickup) {
	
		this.projectPickupService.saveOrUpdate(projectPickup);
		WXUserInfo wXUserInfo = this.wXUserInfoService.updateWXuserInfo(projectPickup);
		model.addAttribute("wXUserInfo", wXUserInfo);
		model.addAttribute("projectPickup", projectPickup);
		
		return "/general/success";
	}
	
	@PostMapping("/stockStatisticsPickup")
	@SystemControllerLog(description = "库存取货")
	public String stockStatisticsPickup(Model model,HttpServletRequest request, @ModelAttribute("stockStatistics") StockStatistics stockStatistics) {
		
		Map<Object, Object> map = this.stockStatisticsService.stockStatisticsPickup(stockStatistics);
		ProjectPickup p = new ProjectPickup();
		p.setOpenId(stockStatistics.getOpenId());
		WXUserInfo wXUserInfo = this.wXUserInfoService.updateWXuserInfo(p);
		model.addAttribute("wXUserInfo", wXUserInfo);
		model.addAttribute("map", map);
		List<StockStatistics> list = this.stockStatisticsService.findByoutboundOrder(map.get("outboundOrder").toString());
		model.addAttribute("list", list);
		
		return "/general/batchOutSuccess";
	}

	
	
	@GetMapping("/findprojectPickup/{id}")
	@SystemControllerLog(description = "查看取货记录")
	public String findprojectPickup(@PathVariable String id ,Model model) {
	

		ProjectPickup projectPickup = this.projectPickupService.findOneById(id, ProjectPickup.class);
		WXUserInfo wXUserInfo = this.wXUserInfoService.updateWXuserInfo(projectPickup);
		model.addAttribute("wXUserInfo", wXUserInfo);
		model.addAttribute("projectPickup", projectPickup);
		
		return "/general/success";
	}

	
	
	
	
	
	
	@RequestMapping(value = "/projectPickupStockStatistics/in", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:in")
	@SystemControllerLog(description = "微信端一键出库")
	public BasicDataResult in(String id, HttpSession session) {
		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		if(Common.isNotEmpty(id)) {
			ProjectPickup projectPickup = this.projectPickupService.findOneById(id, ProjectPickup.class);
			if(projectPickup!=null) {
				
				if(projectPickup.getStock()==null) {
					return new BasicDataResult(400, "出库过程中遇到问题，当前设备需要进行手动出库", null);
				}
				
				StockStatistics stockStatistics = new StockStatistics();
				stockStatistics.setPersonInCharge(projectPickup.getProjectLeader());
				stockStatistics.setProjectName(projectPickup.getEntryName());
				stockStatistics.setCustomer(projectPickup.getItemNo());
				stockStatistics.setNum(projectPickup.getNum());
				stockStatistics.setInOrOut(false);
				stockStatistics.setStock(projectPickup.getStock());
				BasicDataResult inOrOutstockStatistics = this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
				if(inOrOutstockStatistics.getStatus()==200) {
					//出库成功
					projectPickup.setStatus(true);
					this.projectPickupService.saveOrUpdate(projectPickup);
				}
				return new BasicDataResult(inOrOutstockStatistics.getStatus(), inOrOutstockStatistics.getMsg(), null);
			}
		}
		return new BasicDataResult(400, "出库过程中遇到未知错误", null);
		
		
	}
	
	
	
	
}
