package zhongchiedu.wechat.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.InventoryRole;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.GoodsStorageService;
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.inventory.service.PreStockService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;
import zhongchiedu.wx.template.WxMsgPush;

@Controller
@RequestMapping("/wechat")
public class PreStockController {

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	
	private @Autowired GoodsStorageService goodsStorageService;
	
	private @Autowired PreStockService preStockService;

	private @Autowired InventoryRoleService inventoryRoleService;
	
	private @Autowired WxMsgPush wxMsgPush;
	
	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;

	@Autowired
	private StockService stockService;
	
	@Value("${templateId}")
	private String templateId;
	@Value("${templateId2}")
	private String templateId2;
	@Autowired
	private WxMpProperties wxMpProperties;
	
	
	/**
	 * 跳转预入库界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "preStocks", method = RequestMethod.GET)
	@RequiresPermissions(value = "preStock:list")
	@SystemControllerLog(description = "查询所有预入库设备")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,@RequestParam(value = "status", defaultValue = "1") String status,
			@ModelAttribute("errorMsg") String errorMsg) {

		Pagination<PreStock> pagination = this.preStockService.findpagination(pageNo, pageSize, search, "", Integer.valueOf(status));
		model.addAttribute("pageList", pagination);

		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);
		model.addAttribute("errorMsg", errorMsg);
		

		return "preStock/preStock";
	}
	
	
	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/preStock")
	@RequiresPermissions(value = "preStock:add")
	public String addPage(Model model) {
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);

		return "preStock/edit";
	}
	

	@PostMapping(value = "/preStock")
	@RequiresPermissions(value = "preStock:edit")
	public String addStock(Model model,PreStock preStock,HttpSession session) {
		
		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		preStock.setPublisher(user);// 发布人
		String e = preStock.getEstimatedWarehousingTime();
		if(Common.isNotEmpty(e)) {
			String newTime = e.replace("T", " ").substring(0, e.lastIndexOf(":"));
			preStock.setEstimatedWarehousingTime(newTime);
		}
		this.preStockService.saveOrUpdate(preStock);
		
		return "redirect:/wechat/preStocks";
	}
	
	
	
	@DeleteMapping("/preStock/{id}")
	@RequiresPermissions(value = "preStock:delete")
	@SystemControllerLog(description = "删除预入库")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		this.preStockService.delete(id);
		return "redirect:/wechat/preStocks";

	}
	
	
	
	
	
	
	
	
	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/preStock/{id}", method = RequestMethod.GET)
	@RequiresPermissions(value = "preStock:edit")
	@SystemControllerLog(description = "修改预库存信息")
	public String editStock(Model model, @PathVariable String id) {
		PreStock stock = this.preStockService.findOneById(id, PreStock.class);
		
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
	
		model.addAttribute("stock", stock);
		if (Common.isNotEmpty(stock.getArea())) {
			// 所有货架
			List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false, stock.getArea().getId());
			model.addAttribute("goodsStorages", list);
		}

		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		
		
		return "preStock/edit";
	}
	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/preStockToStock/{id}", method = RequestMethod.GET)
//	@RequiresPermissions(value = "preStock:in")
	@SystemControllerLog(description = "跳转添加至预库存界面")
	public String preStockToStock(Model model, @PathVariable String id, HttpServletRequest request,HttpSession session) {
		
		
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
		
		
		
		PreStock stock = this.preStockService.findOneById(id, PreStock.class);
		int status = stock.getStatus();
		//订单已经被处理
		if(status!=1) {
			return "preStock/invalid";
		}
		
		
		
		
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		
		model.addAttribute("stock", stock);
		if (Common.isNotEmpty(stock.getArea())) {
			// 所有货架
			List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false, stock.getArea().getId());
			model.addAttribute("goodsStorages", list);
		}
		
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		
		
		return "preStock/addToStock";
	}
	
	
	//TODO
	/**
	 * 将预库存添加至库存
	 * @param preStock
	 * @param session
	 * @param attr
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@PostMapping("/preStockToStock")
	@RequiresPermissions(value = "preStock:in")
	@SystemControllerLog(description = "预库存添加至库存")
	public String addPreStockAdd(@ModelAttribute("preStock") PreStock preStock, HttpSession session,RedirectAttributes attr)
			throws UnsupportedEncodingException {
		//获取预入库设备状态
				PreStock getpreStock = this.preStockService.findOneById(preStock.getId(), PreStock.class);
				if(getpreStock.getStatus()!=1) {
					//已经处理完
					attr.addFlashAttribute("errorMsg", "当前订单已由他人处理，无需重复添加！");
					return "redirect:/wechat/preStocks";
				}
		
		User suser = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		preStock.setHandler(suser);
		this.stockService.preStockToStock(preStock,getpreStock.getActualReceiptQuantity());

		//创建通知
		Set<User> users = this.inventoryRoleService.findAllUserInInventoryRole();
		

//		InventoryRole inventoryRole = this.inventoryRoleService.findByType("HANDLER");
//		List<User> users = inventoryRole.getUsers();
		
		StringBuilder errorMsg = new StringBuilder("");
		Map<String, String> map = new HashMap<>();
		map.put("first", "设备入库提醒！");
		map.put("keyword1", getpreStock.getName());
		String unit =Common.isNotEmpty(getpreStock.getUnit())?getpreStock.getUnit().getName():"个";
		map.put("keyword2", preStock.getActualReceiptQuantity()+unit);
		if(Common.isNotEmpty(getpreStock.getGoodsStorage())) {
			String area=Common.isNotEmpty(getpreStock.getArea().getName())?getpreStock.getArea().getName():"";
			String address =Common.isNotEmpty(getpreStock.getGoodsStorage().getAddress())? getpreStock.getGoodsStorage().getAddress():"";
			String shelfNumber=Common.isNotEmpty(getpreStock.getGoodsStorage().getShelfNumber())?getpreStock.getGoodsStorage().getShelfNumber():"";
			String shelflevel =Common.isNotEmpty(getpreStock.getGoodsStorage().getShelflevel())?"/"+getpreStock.getGoodsStorage().getShelflevel():"";

			map.put("keyword3", area+"仓库，地址：" + address + "存放在"+shelfNumber+shelflevel);
		}else {
			map.put("keyword3", "设备已经存放在:"+getpreStock.getArea().getName());
		}

		map.put("remark", "预计入库数量："+getpreStock.getEstimatedInventoryQuantity()+"\n实际已入库:"+(preStock.getActualReceiptQuantity()+getpreStock.getActualReceiptQuantity())+"\n入库操作已完成!");
		users.stream().filter(user->Common.isEmpty(user.getOpenId())).forEach(user->{
			errorMsg.append("用户："+user.getUserName()+"尚未绑定微信<BR/>");
		});
		users.stream().filter(user->Common.isNotEmpty(user.getOpenId())).forEach(user->{
			String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId2, user.getOpenId(), "", map);
			if(sendWxMessage=="-1") {
				errorMsg.append("用户："+user.getUserName()+"消息发送失败！<BR/>");
			}
			//			this.wxMsgPush.sendWxMessage(templateId1, "ooiMKv7cqR-2EgkeC9LdATpr-mbY", "www.baidu.com", map);
		});

		attr.addFlashAttribute("errorMsg", errorMsg);

		return "redirect:/wechat/preStocks";
	}
	
	@Value("${qrcode.weburl}")
	private String weburl;

	@RequestMapping(value = "/preStock/preStockPush", method = RequestMethod.POST)
	@RequiresPermissions(value = "preStock:wechatPush")
	@ResponseBody
	public BasicDataResult preStockPush(String id) {
		PreStock preStock = this.preStockService.findOneById(id, PreStock.class);
		InventoryRole inventoryRole = this.inventoryRoleService.findByType("HANDLER");
		List<User> users = inventoryRole.getUsers();
		StringBuilder errorMsg = new StringBuilder("");
		Map<String, String> map = new HashMap<>();
		map.put("first", "预入库通知，待入库完成后请将预入库内容添加至库存！");
		map.put("keyword1", preStock.getName());
		map.put("keyword2", preStock.getModel());
		String model = Common.isNotEmpty(preStock.getModel()) ? "型号：" + preStock.getModel() : "";
//		String estimatedWarehousingTime = Common.isNotEmpty(preStock.getEstimatedWarehousingTime())
//				? "预计将在：" + preStock.getEstimatedWarehousingTime() + "到达"
//				: "";
//		map.put("keyword3", "采购的" + preStock.getName() + model + estimatedWarehousingTime + "请注意查收及入库。");
		map.put("keyword3", preStock.getPublisher().getUserName());
		map.put("remark", "点击此条信息可以通过手机进行入库操作！");
		users.stream().filter(user->Common.isEmpty(user.getOpenId())).forEach(user->{
			errorMsg.append("用户："+user.getUserName()+"尚未绑定微信<BR/>");
		});
		users.stream().filter(user->Common.isNotEmpty(user.getOpenId())).forEach(user->{
			String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId, user.getOpenId(), weburl+"/wechat/preStockToStock/"+preStock.getId(), map);
			if(sendWxMessage=="-1") {
				errorMsg.append("用户："+user.getUserName()+"消息发送失败！<BR/>");
			}
			//			this.wxMsgPush.sendWxMessage(templateId1, "ooiMKv7cqR-2EgkeC9LdATpr-mbY", "www.baidu.com", map);
		});
		if(errorMsg.length()>0) {
			return new BasicDataResult().build(201, "部分人员推送成功", errorMsg);
			
		}
		
		
		return new BasicDataResult().build(200, "消息推送成功", null);
	}
	
	
	

}
