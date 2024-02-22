package zhongchiedu.controller.inventory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
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
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.inventory.service.PickUpApplicationService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.template.WxMsgPush;

/**
 * 设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class PickUpApplicationController {

	private @Autowired PickUpApplicationService pickUpApplicationService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	private @Autowired StockService stockService;

	private @Autowired InventoryRoleService inventoryRoleService;

	private @Autowired UserService userService;

	private @Autowired WxMsgPush wxMsgPush;
	@Value("${templateId3}")
	private String templateId3;
	@Value("${templateId1}")
	private String templateId1;

	@GetMapping("pickUpApplications")
	@RequiresPermissions(value = "pickUpApplication:list")
	@SystemControllerLog(description = "查询所有待出库管理")
	public String prestock(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "status", defaultValue = "1") String status,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea,
			@ModelAttribute("errorMsg") String errorMsg) {

		Pagination<PickUpApplication> pagination = this.pickUpApplicationService.findpagination(pageNo, pageSize,
				search, searchArea, Integer.valueOf(status));
		model.addAttribute("pageList", pagination);

		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);

		session.setAttribute("pickpageNo", pageNo);
		session.setAttribute("pickpageSize", pageSize);
		session.setAttribute("picksearch", search);
		session.setAttribute("picksearchArea", searchArea);
		session.setAttribute("pickstatus", status);
		model.addAttribute("pickpageSize", pageSize);
		model.addAttribute("picksearch", search);
		model.addAttribute("picksearchArea", searchArea);
		model.addAttribute("pickstatus", status);
		model.addAttribute("errorMsg", errorMsg);

		return "admin/pickUpApplication/list";
	}

	/**
	 * 跳转到预库存添加页面
	 */
	@GetMapping("/pickUpApplication")
	@RequiresPermissions(value = "pickUpApplication:add")
	public String addpickUpApplicationPage(Model model) {
		// 所有供应商
//		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
//		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 获取所有用户信息
		List<User> users = this.userService.findAllUser();
		model.addAttribute("users", users);

		return "admin/pickUpApplication/add";
	}

	/**
	 * 跳转到预库存编辑页面
	 */
	@GetMapping("/pickUpApplication{id}")
	@RequiresPermissions(value = "pickUpApplication:edit")
	public String editEstimatePage(Model model, @PathVariable String id) {
		// 所有供应商
//		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
//		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		PickUpApplication stock = this.pickUpApplicationService.findOneById(id, PickUpApplication.class);
		model.addAttribute("pickUpApplication", stock);
		List<Stock> stocks = this.stockService.findAllStock(false, stock.getArea().getId(),"");
		model.addAttribute("stocks", stocks);
		// 获取所有用户信息
		List<User> users = this.userService.findAllUser();
		model.addAttribute("users", users);

		return "admin/pickUpApplication/add";
	}

	/**
	 * 跳转到预库存添加页面
	 */
	@GetMapping("/pickUpApplicationAdd{id}")
	@RequiresPermissions(value = "pickUpApplication:add")
	public String inStockPage(Model model, @PathVariable String id) {

		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		PickUpApplication stock = this.pickUpApplicationService.findOneById(id, PickUpApplication.class);
		model.addAttribute("pickUpApplication", stock);
		List<Stock> stocks = this.stockService.findAllStock(false, stock.getArea().getId(),"");
		model.addAttribute("stocks", stocks);
		// 获取所有用户信息
		List<User> users = this.userService.findAllUser();
		model.addAttribute("users", users);

		return "admin/pickUpApplication/pickUpAdd";
	}

	@PostMapping("/pickUpApplication")
	@RequiresPermissions(value = "pickUpApplication:add")
	@SystemControllerLog(description = "添加预库存")
	public String addPickUpApplication(@ModelAttribute("pickUpApplication") PickUpApplication pickUpApplication,
			HttpSession session)

			throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		pickUpApplication.setPublisher(user);// 发布人
		this.pickUpApplicationService.saveOrUpdate(pickUpApplication);
		Integer pageNo = (Integer) session.getAttribute("pickpageNo");
		Integer pageSize = (Integer) session.getAttribute("pickpageSize");
		String search = (String) session.getAttribute("picksearch");
		String searchArea = (String) session.getAttribute("picksearchArea");
		return "redirect:/pickUpApplications?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;

	}

	@PutMapping("/pickUpApplicationAdd")
	@RequiresPermissions(value = "pickUpApplication:out")
	@SystemControllerLog(description = "库存完成出库")
	@ResponseBody
	public BasicDataResult addPickUpApplicationAdd(
			@ModelAttribute("pickUpApplication") PickUpApplication pickUpApplication, HttpSession session)
			throws UnsupportedEncodingException {
	//优化前
//		if (pickUpApplication.getActualIssueQuantity() <= 0) {
//			return new BasicDataResult().build(400, "出库数量有误！", "出库数量有误！");
//		}
		//通过num 来判断实际出库数量
		
		if (pickUpApplication.getNum() <= 0) {
			return new BasicDataResult().build(400, "出库数量有误！", "出库数量有误！");
		}

		PickUpApplication getpickUpApplication = this.pickUpApplicationService.findOneById(pickUpApplication.getId(),
				PickUpApplication.class);
		
		long estimatedIssueQuantity = getpickUpApplication.getEstimatedIssueQuantity();//预计出库数量
		long actualIssueQuantity = getpickUpApplication.getActualIssueQuantity();//实际出库数量
		
		long newNum = estimatedIssueQuantity - actualIssueQuantity;
		if (pickUpApplication.getNum() > newNum) {
			return new BasicDataResult().build(400, "出库数量不能超过剩余数量！", "出库数量不能超过剩余数量");
		}
		
		
		int status = getpickUpApplication.getStatus();
		if (status != 1 && status !=3) {
			return new BasicDataResult().build(400, "不能重复出库", "不能重复出库");
		}
		User suser = (User) session.getAttribute(Contents.USER_SESSION);
		pickUpApplication.setHandler(suser);
		BasicDataResult pickUpApplicationToStock = this.stockService.pickUpApplicationToStock(pickUpApplication);

		if (pickUpApplicationToStock.getStatus() == 200) {
			// 出库成功 推送消息

			// 创建通知
			InventoryRole inventoryRole = this.inventoryRoleService.findByType("HANDLER");
			List<User> users = inventoryRole.getUsers();
//			String personInCharge = getpickUpApplication.getPersonInCharge();
//			if(Common.isNotEmpty(personInCharge)) {
//				String[] split = personInCharge.split(",");
//				 List<Object> ids = Arrays.asList(split).stream().map(x->new ObjectId(x)).collect(Collectors.toList());
//				List<User> users = this.userService.findUserInIds(ids);
//				String person= users.stream().map(x->x.getUserName()).collect(Collectors.joining(", "));

				StringBuilder errorMsg = new StringBuilder("");
				Map<String, String> map = new HashMap<>();
				map.put("first", "设备出库提醒！");
				map.put("keyword1", getpickUpApplication.getStock().getName());
				map.put("keyword2", String.valueOf(pickUpApplication.getActualIssueQuantity()));
				map.put("keyword3", getpickUpApplication.getCustomer());
				map.put("keyword4", getpickUpApplication.getPersonInCharge());
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
				return new BasicDataResult().build(200, "出库成功", errorMsg);

				
//			}
			
		}
		return pickUpApplicationToStock;

//
//		Integer pageNo = (Integer) session.getAttribute("pickpageNo");
//		Integer pageSize = (Integer) session.getAttribute("pickpageSize");
//		String search = (String) session.getAttribute("picksearch");
//		String searchArea = (String) session.getAttribute("picksearchArea");
//		String status = "2";
//		return "redirect:/pickUpApplications?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
//				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status;

	}

	@PutMapping("/pickUpApplication")
	@RequiresPermissions(value = "pickUpApplication:edit")
	@SystemControllerLog(description = "修改预出库")
	public String editPickUpApplication(@ModelAttribute("pickUpApplication") PickUpApplication pickUpApplication,
			HttpSession session) throws UnsupportedEncodingException {
		this.pickUpApplicationService.saveOrUpdate(pickUpApplication);
		Integer pageNo = (Integer) session.getAttribute("pickpageNo");
		Integer pageSize = (Integer) session.getAttribute("pickpageSize");
		String search = (String) session.getAttribute("picksearch");
		String searchArea = (String) session.getAttribute("picksearchArea");

		return "redirect:/pickUpApplications?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;

	}

	@DeleteMapping("/pickUpApplication/{id}")
	@RequiresPermissions(value = "pickUpApplication:delete")
	@SystemControllerLog(description = "删除预库存")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		Integer pageNo = (Integer) session.getAttribute("pickpageNo");
		Integer pageSize = (Integer) session.getAttribute("pickpageSize");
		String search = (String) session.getAttribute("picksearch");
		String searchArea = (String) session.getAttribute("picksearchArea");
		String status = (String) session.getAttribute("pickstatus");
		log.info("删除设备" + id);
		this.pickUpApplicationService.delete(id);
		log.info("删除设备" + id + "成功");
		return "redirect:/pickUpApplications?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea + "&status=" + status;
	}

	@RequestMapping("/pickUpApplication/clearSearch")
//	@RequiresPermissions(value = "projectStock:delete")
	public String clearSearch(HttpSession session) {
		session.removeAttribute("pageNo");
		session.removeAttribute("pageSize");
		session.removeAttribute("search");
		session.removeAttribute("searchArea");
		return "redirect:/pickUpApplications";

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
		
		//List<String> userNames = new ArrayList<>();
//		if(Common.isNotEmpty(pickUpApplication.getPersonInCharge())) {
//			
//			List<User> personIncharge = this.userService.getUsersByIds(pickUpApplication.getPersonInCharge());
//			 userNames = personIncharge.stream().map(x ->x.getUserName()).collect(Collectors.toList());
//		}
		
		StringBuilder errorMsg = new StringBuilder("");
		Map<String, String> map = new HashMap<>();
		map.put("first", "预出库通知，有设备即将出库！");
		map.put("keyword1", pickUpApplication.getStock().getName());
		map.put("keyword2", pickUpApplication.getId());

		String person = Common.isNotEmpty(pickUpApplication.getPersonInCharge())
				? "\n负责人：" + pickUpApplication.getPersonInCharge() : "";
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
			return new BasicDataResult().build(200, "获取设备信息成功", stocks);
		}
		return new BasicDataResult().build(400, "获取设备信息失败", stocks);
	}

	/**
	 * 库存数量判断
	 * 
	 * @param areaId
	 * @return
	 */
	@RequestMapping(value = "checkStockNum", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult checkStockNum(String stockId, String num) {
		if (Common.isEmpty(stockId)) {
			return new BasicDataResult().build(400, "未能获取出库设备信息", "");
		}
		if (Common.isEmpty(num)) {
			return new BasicDataResult().build(400, "未能获取到出库数量", "");
		}

		Stock stock = this.stockService.findOneById(stockId, Stock.class);
		Long getnum = Long.valueOf(num);
		if (getnum > stock.getInventory()) {
			return new BasicDataResult().build(400, "库存数量不足，当前剩余库存为:" + stock.getInventory(), "");
		}
		return new BasicDataResult().build(200, "出库数量无误", "");

	}

	public static void main(String[] args) {
		StringBuilder errorMsg = new StringBuilder();
		System.out.println(errorMsg.length() == 0);
	}

}
