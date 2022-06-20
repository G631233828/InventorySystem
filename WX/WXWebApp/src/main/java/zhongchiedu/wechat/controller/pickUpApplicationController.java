package zhongchiedu.wechat.controller;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.chanjar.weixin.mp.api.WxMpService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.InventoryRole;
import zhongchiedu.inventory.pojo.PickUpApplication;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.service.GoodsStorageService;
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.inventory.service.PickUpApplicationService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;
import zhongchiedu.wx.template.WxMsgPush;

/**
 * 预出库登记
 * 
 * @author fliay
 *
 */
@Controller
@RequestMapping("/wechat")
public class pickUpApplicationController {

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	private @Autowired GoodsStorageService goodsStorageService;

	private @Autowired PickUpApplicationService pickUpApplicationService;

	private @Autowired InventoryRoleService inventoryRoleService;

	private @Autowired WxMsgPush wxMsgPush;

	private @Autowired UserService userService;
	@Autowired
	private StockService stockService;

	@Value("${templateId3}")
	private String templateId3;
	@Value("${templateId1}")
	private String templateId1;

	@Autowired
	private WxMpProperties wxMpProperties;

	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;

	/**
	 * 跳转预出库界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "pickUpApplications", method = RequestMethod.GET)
	@RequiresPermissions(value = "pickUpApplication:list")
	@SystemControllerLog(description = "查询所有预出库设备")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "status", defaultValue = "1") String status,
			@ModelAttribute("errorMsg") String errorMsg) {

		Pagination<PickUpApplication> pagination = this.pickUpApplicationService.findpagination(pageNo, pageSize,
				search, "", Integer.valueOf(status));
		model.addAttribute("pageList", pagination);

		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);
		model.addAttribute("errorMsg", errorMsg);

		return "pickUpApplication/pickUpApplication";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/pickUpApplication")
	@RequiresPermissions(value = "pickUpApplication:add")
	public String addPage(Model model) {
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);

		// 获取所有用户信息
		List<User> users = this.userService.findAllUser();
		List<Object> list = new ArrayList();
		users.stream().forEach(x -> {
			Map<String, Object> m = new HashMap<>();
			m.put("title", x.getUserName());
			m.put("value", x.getId());
			m.put("description", x.getDescription());
			list.add(m);
		});
		model.addAttribute("users", list);

		return "pickUpApplication/edit";
	}

	@PostMapping(value = "/pickUpApplication")
	@RequiresPermissions(value = "pickUpApplication:edit")
	public String addStock(Model model, PickUpApplication pickUpApplication, HttpSession session) {

		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		pickUpApplication.setPublisher(user);// 发布人
		String e = pickUpApplication.getPickUpTime();
		if (Common.isNotEmpty(e)) {
			String newTime = e.replace("T", " ").substring(0, e.lastIndexOf(":"));
			pickUpApplication.setPickUpTime(newTime);
		}

		this.pickUpApplicationService.saveOrUpdate(pickUpApplication);

		return "redirect:/wechat/pickUpApplications";
	}

	@DeleteMapping("/pickUpApplication/{id}")
	@RequiresPermissions(value = "pickUpApplication:delete")
	@SystemControllerLog(description = "删除预出库")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		this.pickUpApplicationService.delete(id);
		return "redirect:/wechat/pickUpApplications";

	}

	/**
	 * 根据区域获取库存信息
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "getStocks", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult getStocks(String areaId) {
		System.out.println(areaId);
		if (Common.isEmpty(areaId)) {
			return new BasicDataResult().build(400, "未能获取到设备信息", "");
		}
		// 通过areaid获取库存
		List<Stock> stocks = this.stockService.findAllStock(false, areaId,"");
		if (stocks.size() > 0) {
			int stockSize = stocks.size();
			String rstock[] = new String[stockSize];
			// 获取数据拼接返回成界面需要的格式 ['a,b,c','d,e,f']
			for (int i = 0; i < stocks.size(); i++) {
				Stock s = stocks.get(i);
				String e = s.getId() + "," + s.getName() + "," + s.getModel();
				rstock[i] = e;
			}

			return new BasicDataResult().build(200, "获取设备信息成功", rstock);
		}
		return new BasicDataResult().build(400, "获取设备信息失败", "");
	}

	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/pickUpApplication/{id}", method = RequestMethod.GET)
	@RequiresPermissions(value = "pickUpApplication:edit")
	@SystemControllerLog(description = "修改预库存信息")
	public String editStock(Model model, @PathVariable String id) {

		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		PickUpApplication stock = this.pickUpApplicationService.findOneById(id, PickUpApplication.class);
		model.addAttribute("stock", stock);

		// 获取所有用户信息
		List<User> users = this.userService.findAllUser();
		List<Object> list = new ArrayList<>();
		List<String> names = new ArrayList<>();
		users.stream().forEach(x -> {
			Map<String, Object> m = new HashMap<>();
			m.put("title", x.getUserName());
			m.put("value", x.getId());
			m.put("description", x.getDescription());
			if (stock.getPersonInCharge().contains(x.getId())) {
				names.add(x.getUserName());
			}
			list.add(m);
		});
		model.addAttribute("users", list);

		model.addAttribute("name", names.toString().replace("[", "").replace("]", "").replaceAll(" ", ""));

		// 通过areaid获取库存
		List<Stock> stocks = this.stockService.findAllStock(false, stock.getArea().getId(),"");
		if (stocks.size() > 0) {
			int stockSize = stocks.size();
			String rstock[] = new String[stockSize];
			// 获取数据拼接返回成界面需要的格式 ['a,b,c','d,e,f']
			for (int i = 0; i < stocks.size(); i++) {
				Stock s = stocks.get(i);
				String e = s.getId() + "," + s.getName() + "," + s.getModel();
				rstock[i] = e;
			}
			model.addAttribute("source", rstock);
		}

		return "pickUpApplication/edit";
	}

	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/pickUpApplicationToStock/{id}", method = RequestMethod.GET)
//	@RequiresPermissions(value = "pickUpApplication:out")
	@SystemControllerLog(description = "跳转添加至预出库界面")
	public String pickUpApplicationToStock(Model model, @PathVariable String id, HttpSession session,
			HttpServletRequest request) {
		// 获取微信端code
		String code = request.getParameter("code");
		// 判断code是否为空，如果为空的话需要通过微信进行重定向
		if (Common.isEmpty(code)) {
			String redirect_uri = wxMpProperties.getServerUrl() + request.getRequestURI();
//				return new ModelAndView(new RedirectView("https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
//						+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
//						+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect"));
			return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
					+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
					+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
		}
		PickUpApplication stock = this.pickUpApplicationService.findOneById(id, PickUpApplication.class);

		// 获取所有用户信息
		List<User> users = this.userService.findAllUser();
		List<Object> list = new ArrayList<>();
		List<String> names = new ArrayList<>();
		users.stream().forEach(x -> {
			Map<String, Object> m = new HashMap<>();
			m.put("title", x.getUserName());
			m.put("value", x.getId());
			m.put("description", x.getDescription());
			if (stock.getPersonInCharge()!=null&&stock.getPersonInCharge().contains(x.getId())) {
				names.add(x.getUserName());
			}
			list.add(m);
		});
		model.addAttribute("users", list);

		model.addAttribute("name", names.toString().replace("[", "").replace("]", "").replaceAll(" ", ""));

		int status = stock.getStatus();// 获取处理状态如果不是1说明已经处理完场
		if (status == 1) {
			List<Area> areas = this.areaService.findAllArea(false);
			model.addAttribute("areas", areas);
			// PickUpApplication stock = this.pickUpApplicationService.findOneById(id,
			// PickUpApplication.class);
			model.addAttribute("stock", stock);

			return "pickUpApplication/pickUpAdd";
		}

		return "pickUpApplication/invalid";
	}

	/**
	 * 将预库存添加至库存
	 * 
	 * @param pickUpApplication
	 * @param session
	 * @param attr
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@PostMapping("/pickUpApplicationAdd")
	@RequiresPermissions(value = "pickUpApplication:out")
	@SystemControllerLog(description = "预出库完成出库")
	public String addPickUpApplicationAdd(@ModelAttribute("pickUpApplication") PickUpApplication pickUpApplication,
			HttpSession session, RedirectAttributes attr) throws UnsupportedEncodingException {
		User suser = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		pickUpApplication.setHandler(suser);
		BasicDataResult result = this.stockService.pickUpApplicationToStock(pickUpApplication);
		// 判断是否出库成功
		if (result.getStatus() == 200) {
			pickUpApplication = this.pickUpApplicationService.findOneById(pickUpApplication.getId(),
					PickUpApplication.class);
			// 创建通知
//			Set<User> users = this.inventoryRoleService.findAllUserInInventoryRole();
			String personInCharge = pickUpApplication.getPersonInCharge();
			
			if(Common.isNotEmpty(personInCharge)) {
				List<User> users = this.userService.getUsersByIds(personInCharge);
				List<String> userNames = users.stream().map(x ->x.getUserName()).collect(Collectors.toList());
				StringBuilder errorMsg = new StringBuilder("");
				Map<String, String> map = new HashMap<>();

				map.put("first", "设备出库提醒！");
				map.put("keyword1", pickUpApplication.getStock().getName());
				map.put("keyword2", String.valueOf(pickUpApplication.getActualIssueQuantity()));
				map.put("keyword3", pickUpApplication.getCustomer());
				map.put("keyword4", userNames.toString().replace("[", "").replace("]", "").replaceAll(" ", ""));
				map.put("remark", "设备出库已完成");
				users.stream().filter(user -> Common.isEmpty(user.getOpenId())).forEach(user -> {
					errorMsg.append("用户：" + user.getUserName() + "尚未绑定微信<BR/>");
				});
				users.stream().filter(user -> Common.isNotEmpty(user.getOpenId())).forEach(user -> {
					String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId3, user.getOpenId(), "", map);
					if (sendWxMessage == "-1") {
						errorMsg.append("用户：" + user.getUserName() + "消息发送失败！<BR/>");
					}
					// this.wxMsgPush.sendWxMessage(templateId1, "ooiMKv7cqR-2EgkeC9LdATpr-mbY",
					// "www.baidu.com", map);
				});
				attr.addFlashAttribute("errorMsg", errorMsg);
			}
			

		} else {
			attr.addFlashAttribute("errorMsg", result.getMsg());
		}

//		Integer pageNo = (Integer) session.getAttribute("prepageNo");
//		Integer pageSize = (Integer) session.getAttribute("prepageSize");
//		String search = (String) session.getAttribute("presearch");
//		String searchArea = (String) session.getAttribute("presearchArea");
//		String status = "2";
//		return "redirect:/pickUpApplications?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
//				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status;
		return "redirect:/wechat/pickUpApplications";
	}


	@Value("${qrcode.weburl}")
	private String weburl;

	@RequestMapping(value = "/pickUpApplication/pickUpApplicationPush", method = RequestMethod.POST)
	@RequiresPermissions(value = "pickUpApplication:wechatPush")
	@ResponseBody
	public BasicDataResult pickUpApplicationPush(String id) {
		PickUpApplication pickUpApplication = this.pickUpApplicationService.findOneById(id, PickUpApplication.class);
		
		
		InventoryRole inventoryRole = this.inventoryRoleService.findByType("HANDLER");
		List<User> users = inventoryRole.getUsers();
		List<String> userNames = new ArrayList<>();
		if(Common.isNotEmpty(pickUpApplication.getPersonInCharge())) {
			
			List<User> personIncharge = this.userService.getUsersByIds(pickUpApplication.getPersonInCharge());
			 userNames = personIncharge.stream().map(x ->x.getUserName()).collect(Collectors.toList());
		}
	
		
		StringBuilder errorMsg = new StringBuilder("");
		Map<String, String> map = new HashMap<>();
		map.put("first", "预出库通知，有设备即将出库！");
		map.put("keyword1", pickUpApplication.getStock().getName());
		map.put("keyword2", pickUpApplication.getId());

		String person = Common.isNotEmpty(pickUpApplication.getPersonInCharge())
				? "\n负责人：" + userNames.toString().replace("[", "").replace("]", "").replaceAll(" ", "")
				: "";
		map.put("keyword3", "预计出库数量为：" + pickUpApplication.getEstimatedIssueQuantity() + person);
		map.put("remark", "点击此条信息可以通过手机进行出库操作！");
		users.stream().filter(user -> Common.isEmpty(user.getOpenId())).forEach(user -> {
			errorMsg.append("用户：" + user.getUserName() + "尚未绑定微信<BR/>");
		});
		users.stream().filter(user -> Common.isNotEmpty(user.getOpenId())).forEach(user -> {
			String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId1, user.getOpenId(),
					weburl + "/wechat/pickUpApplicationToStock/" + pickUpApplication.getId(), map);
			if (sendWxMessage == "-1") {
				errorMsg.append("用户：" + user.getUserName() + "消息发送失败！<BR/>");
			}
			// this.wxMsgPush.sendWxMessage(templateId1, "ooiMKv7cqR-2EgkeC9LdATpr-mbY",
			// "www.baidu.com", map);
		});
		if (errorMsg.length() > 0) {
			return new BasicDataResult().build(201, "部分人员推送成功", errorMsg);

		}

		return new BasicDataResult().build(200, "消息推送成功", null);
	}

}
