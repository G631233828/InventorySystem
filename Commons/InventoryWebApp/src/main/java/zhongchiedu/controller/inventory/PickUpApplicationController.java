package zhongchiedu.controller.inventory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.InventoryRole;
import zhongchiedu.inventory.pojo.NewCustomer;
import zhongchiedu.inventory.pojo.PickUpApplication;
import zhongchiedu.inventory.pojo.Pname;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.inventory.service.NewCustomerService;
import zhongchiedu.inventory.service.PickUpApplicationService;
import zhongchiedu.inventory.service.PnameService;
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

	private @Autowired PnameService pnameService;

	private @Autowired NewCustomerService newCustomerService;
	
	
	private @Autowired RedisTemplate redisTemplate;

	@Value("${templateId3}")
	private String templateId3;
	@Value("${templateId1}")
	private String templateId1;

	@GetMapping("pickUpApplications")
	@RequiresPermissions(value = "pickUpApplication:list")
	@SystemControllerLog(description = "查询所有待出库管理")
	public String prestock(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@RequestParam(value = "status", defaultValue = "") String status,
			@RequestParam(value = "pnameid", defaultValue = "") String pnameid,
			@RequestParam(value = "customerid", defaultValue = "") String customerid,
			@RequestParam(value = "stockid", defaultValue = "") String stockid,
			@RequestParam(value = "modelid", defaultValue = "") String modelid,
			@RequestParam(value = "publisherid", defaultValue = "") String publisherid,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea,
			@ModelAttribute("errorMsg") String errorMsg) {

		
		
//		Set<String> set = (Set<String>) this.redisTemplate.opsForValue().get("projectNames");
//		if (Common.isNotEmpty(set)) {
//			return set;
//		}
//		List<StockStatistics> findAllStockStatics = this.stockStatisticsService.findAllStockStatics();
//		for (StockStatistics stocks : findAllStockStatics) {
//			if (Common.isNotEmpty(stocks.getProjectName())) {
//				projects.add(stocks.getProjectName());
//			}
//		}
//		this.redisTemplate.opsForValue().set("projectNames", projects);
		
		Pagination<PickUpApplication> pagination = this.pickUpApplicationService.findpagination(pageNo, pageSize,
				 searchArea, status,pnameid,customerid,stockid,modelid,publisherid);
		
		List<Pname> pnames = (List<Pname>) this.redisTemplate.opsForValue().get("allpname");
		if (Common.isNotEmpty(pnames)) {
			model.addAttribute("pnames", pnames);
		}else {
			List<Pname> findAllName = this.pnameService.findAllName(false);
			model.addAttribute("pnames", findAllName);
			this.redisTemplate.opsForValue().set("allpname", findAllName);
			this.redisTemplate.expire("allpname", 20, TimeUnit.MINUTES);
		}
		
		List<NewCustomer> customers = (List<NewCustomer>) this.redisTemplate.opsForValue().get("allcustomer");
		if (Common.isNotEmpty(customers)) {
			model.addAttribute("customers", customers);
		}else {
			// 获取到所有客户
			List<NewCustomer> findAllCustomer = this.newCustomerService.findAllCustomer(false);
			model.addAttribute("customers", findAllCustomer);
			this.redisTemplate.opsForValue().set("allcustomer", findAllCustomer);
			this.redisTemplate.expire("allcustomer", 20, TimeUnit.MINUTES);
		}
		
		
		
	
	
		Query query=new Query();
		query.addCriteria(Criteria.where("cardId").is("publisher"));
		List<User> users=userService.find(query,User.class);
		model.addAttribute("publishers",users);
		
//		List<PickUpApplication> findAllPickUpApplication = this.pickUpApplicationService.findAllPickUpApplication(false, null);
//		
//		Set<Stock> stocklists = findAllPickUpApplication.stream().map(PickUpApplication::getStock).collect(Collectors.toCollection(LinkedHashSet::new));
//		model.addAttribute("stocklists", stocklists);
		
		
		Set<Stock> redisstock = (Set<Stock>) this.redisTemplate.opsForValue().get("stocklists");
		if (Common.isNotEmpty(redisstock)) {
			model.addAttribute("stocklists", redisstock);
		}else {
			// 获取到所有客户
			List<PickUpApplication> findAllPickUpApplication = this.pickUpApplicationService.findAllPickUpApplication(false, null);
			
			Set<Stock> stocklists = findAllPickUpApplication.stream().map(PickUpApplication::getStock).collect(Collectors.toCollection(LinkedHashSet::new));
			model.addAttribute("stocklists", stocklists);
			this.redisTemplate.opsForValue().set("stocklists", stocklists);
			this.redisTemplate.expire("stocklists", 20, TimeUnit.MINUTES);
		}
		
		
		
		
		
		
		
		
		
//		Collection<Stock> stocks = findAllPickUpApplication.stream()
//	                .map(PickUpApplication::getStock) // 获取每个PickUpApplication中的Stock对象
//	                .collect(Collectors.toMap(
//	                        Stock::getId, // 作为Map的键
//	                        Function.identity(), // 作为Map的值，直接返回Stock对象
//	                        (existing, replacement) -> existing, // 合并函数，这里我们保留现有的对象
//	                        LinkedHashMap::new // 使用LinkedHashMap来保持插入顺序
//	                )).values(); // 获取Map的值，即不重复的Stock集合
		
		model.addAttribute("pageList", pagination);

		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		
		
		
		
		
		
		
		model.addAttribute("errorImport", errorImport);
		model.addAttribute("pickpageSize", pageSize);
		model.addAttribute("picksearchArea", searchArea);
		model.addAttribute("pickstatus", status);
		model.addAttribute("errorMsg", errorMsg);
		session.setAttribute("selectcustomer", customerid);
		session.setAttribute("selectpname", pnameid);
		session.setAttribute("stockid", stockid);
		session.setAttribute("modelid", modelid);
		session.setAttribute("searchArea", searchArea);
		session.setAttribute("pickpageNo", pageNo);
		session.setAttribute("pickpageSize", pageSize);
		session.setAttribute("picksearchArea", searchArea);
		session.setAttribute("pickstatus", status);
		session.setAttribute("publisherid",publisherid);

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
//		List<User> users = this.userService.findAllUser();
//		model.addAttribute("users", users);
		// 获取到所有项目名称
		List<Pname> findAllName = this.pnameService.findAllName(false);
		model.addAttribute("pnames", findAllName);
		// 获取到所有客户
		List<NewCustomer> findAllCustomer = this.newCustomerService.findAllCustomer(false);
		model.addAttribute("customers", findAllCustomer);

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
		List<Stock> stocks = this.stockService.findAllStock(false, stock.getArea().getId(), "");
		model.addAttribute("stocks", stocks);
		// 获取所有用户信息
//		List<User> users = this.userService.findAllUser();
//		model.addAttribute("users", users);
		// 获取到所有项目名称
		List<Pname> findAllName = this.pnameService.findAllName(false);
		model.addAttribute("pnames", findAllName);
		// 获取到所有客户
		List<NewCustomer> findAllCustomer = this.newCustomerService.findAllCustomer(false);
		model.addAttribute("customers", findAllCustomer);

		return "admin/pickUpApplication/add";
	}

	/**
	 * 跳转到预库存添加页面
	 */
	@GetMapping("/pickUpApplicationAdd{id}")
//	@RequiresPermissions(value = "pickUpApplication:add")
	public String inStockPage(Model model, @PathVariable String id) {

		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		PickUpApplication stock = this.pickUpApplicationService.findOneById(id, PickUpApplication.class);
		model.addAttribute("pickUpApplication", stock);
		List<Stock> stocks = this.stockService.findAllStock(false, stock.getArea().getId(), "");
		model.addAttribute("stocks", stocks);
		// 获取所有用户信息
//		List<User> users = this.userService.findAllUser();
//		model.addAttribute("users", users);
		List<Pname> findAllName = this.pnameService.findAllName(false);
		model.addAttribute("pnames", findAllName);
		// 获取到所有客户
		List<NewCustomer> findAllCustomer = this.newCustomerService.findAllCustomer(false);
		model.addAttribute("customers", findAllCustomer);

		return "admin/pickUpApplication/pickUpAdd";
	}

	@PostMapping("/pickUpApplication")
	@RequiresPermissions(value = "pickUpApplication:add")
	@SystemControllerLog(description = "添加预出库")
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
		return "redirect:pickUpApplications";

	}

	@PutMapping("/pickUpApplicationAdd")
//	@RequiresPermissions( value={"pickUpApplication:out","pickUpApplication:add"},logical=Logical.OR)
	@SystemControllerLog(description = "库存完成出库")
	@ResponseBody
	public BasicDataResult addPickUpApplicationAdd(
			@ModelAttribute("pickUpApplication") PickUpApplication pickUpApplication, HttpSession session)
			throws UnsupportedEncodingException {
		// 优化前
//		if (pickUpApplication.getActualIssueQuantity() <= 0) {
//			return new BasicDataResult().build(400, "出库数量有误！", "出库数量有误！");
//		}
		// 通过num 来判断实际出库数量

		if (pickUpApplication.getNum() <= 0) {
			return new BasicDataResult().build(400, "出库数量有误！", "出库数量有误！");
		}

		PickUpApplication getpickUpApplication = this.pickUpApplicationService.findOneById(pickUpApplication.getId(),
				PickUpApplication.class);

		long estimatedIssueQuantity = getpickUpApplication.getEstimatedIssueQuantity();// 预计出库数量
		long actualIssueQuantity = getpickUpApplication.getActualIssueQuantity();// 实际出库数量

		long newNum = estimatedIssueQuantity - actualIssueQuantity;
		if (pickUpApplication.getNum() > newNum) {
			return new BasicDataResult().build(400, "出库数量不能超过剩余数量！", "出库数量不能超过剩余数量");
		}

		int status = getpickUpApplication.getStatus();
		if (status != 1 && status != 3) {
			return new BasicDataResult().build(400, "设备可出库数量为0", "设备可出库数量为0");
		}
		User suser = (User) session.getAttribute(Contents.USER_SESSION);
		pickUpApplication.setHandler(suser);
		pickUpApplication.setPersonInCharge(getpickUpApplication.getPersonInCharge());
		pickUpApplication.setNewCustomer(getpickUpApplication.getNewCustomer());
		pickUpApplication.setPname(getpickUpApplication.getPname());
		BasicDataResult pickUpApplicationToStock = this.stockService.pickUpApplicationToStock(pickUpApplication);

		if (pickUpApplicationToStock.getStatus() == 200) {
			// 出库成功 推送消息

			// 创建通知
			InventoryRole inventoryRole = this.inventoryRoleService.findByType("HANDLER");
			if (Common.isEmpty(inventoryRole)) {
				return new BasicDataResult().build(200, "出库成功", "未获得绑定微信人员信息");
			}
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
			map.put("keyword3", getpickUpApplication.getNewCustomer().getName());
			map.put("keyword4", getpickUpApplication.getPname().getPm());
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

		return "redirect:pickUpApplications";

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
		
		search = Common.isNotEmpty(search)?URLEncoder.encode(search, "UTF-8"):"";
				
		log.info("删除设备" + id);
		this.pickUpApplicationService.delete(id);
		log.info("删除设备" + id + "成功");
		return "redirect:/pickUpApplications?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+search + "&searchArea=" + searchArea + "&status=" + status;
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

		if (Common.isEmpty(inventoryRole)) {
			return new BasicDataResult().build(201, "消息推送失败，清先绑定人员", null);
		}
		List<User> users = inventoryRole.getUsers();

		// List<String> userNames = new ArrayList<>();
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
				? "\n负责人：" + pickUpApplication.getPersonInCharge()
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

	/**
	 * 根据区域获取库存信息
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "getStocks", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult getStocks(String areaId) {
		if (Common.isEmpty(areaId)) {
			return new BasicDataResult().build(400, "未能获取到设备信息", "");
		}
		// 通过areaid获取库存
		List<Stock> stocks = this.stockService.findAllStock(false, areaId, "");
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
	public BasicDataResult checkStockNum(String stockId, String num,String pickId) {
		
		long self= 0L;
		if (Common.isEmpty(stockId)) {
			return new BasicDataResult().build(400, "未能获取出库设备信息", "");
		}
		if (Common.isEmpty(num)) {
			return new BasicDataResult().build(400, "未能获取到出库数量", "");
		}
		long getnum = Long.valueOf(num);
		
		if(Common.isNotEmpty(pickId)) {
			PickUpApplication self_ = this.pickUpApplicationService.findOneById(pickId, PickUpApplication.class);
			if(Common.isNotEmpty(self_)) {
				self = self_.getEstimatedIssueQuantity();
				if(getnum <= self) {
					return new BasicDataResult().build(200, "出库数量无误", "");
				}
			}
		}
		

		Stock stock = this.stockService.findOneById(stockId, Stock.class);

		List<PickUpApplication> pickUpApplication = this.pickUpApplicationService
				.findPickUpApplicationsByStockId(stock.getId());
		long ycknum = pickUpApplication.stream().map(PickUpApplication::getEstimatedIssueQuantity).reduce((long) 0,
				Long::sum);
		long acnum = pickUpApplication.stream().map(PickUpApplication::getActualIssueQuantity).reduce((long) 0,
				Long::sum);

		
		if (getnum > (stock.getInventory() - (ycknum- self - acnum))) {
			return new BasicDataResult().build(400, "库存数量不足，当前剩余可出库数量为:" + (stock.getInventory() - (ycknum - self - acnum)),
					"");
		}
		return new BasicDataResult().build(200, "出库数量无误", "");

	}

	@RequestMapping(value = "/pickUpApplication/batchOut", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
//	@RequiresPermissions(value = {"stockStatistics:out","stockStatistics:yout","pickUpApplication:out"},logical = Logical.OR)
	@SystemControllerLog(description = "批量预出库")
	public BasicDataResult batchOut(String batchid, String batchnum, String batchdescription, String pname,String personInCharge,
			String newCustomer, String accepter, HttpSession session) {

		String[] ids = batchid.split(",");
		String[] nums = batchnum.split(",");
		List<String> batchidList = Arrays.asList(ids);
		List<String> batchnumList = Arrays.asList(nums);

		if (batchidList.size() != batchnumList.size()) {
			return new BasicDataResult(400, "出库商品与id不匹配", "");
		}
		if (Common.isEmpty(pname)) {
			return new BasicDataResult(400, "项目名称不能为空", "");
		}
		if (Common.isEmpty(newCustomer)) {
			return new BasicDataResult(400, "客户不能为空", "");
		}
		if (Common.isEmpty(accepter)) {
			return new BasicDataResult(400, "领料人不能为空", "");
		}

		User user = (User) session.getAttribute(Contents.USER_SESSION);
		String orderNum = Common.getOrderNum();
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < batchidList.size(); i++) {
			Stock stock = this.stockService.findOneById(batchidList.get(i), Stock.class);

			PickUpApplication pick = new PickUpApplication();

			pick.setPublisher(user);// 发布人
			pick.setStock(stock);
			pick.setArea(stock.getArea());
			Pname p = new Pname();
			p.setId(pname);
			pick.setPname(p);
			NewCustomer c = new NewCustomer();
			c.setId(newCustomer);
			pick.setNewCustomer(c);
			pick.setPersonInCharge(personInCharge);
			pick.setEstimatedIssueQuantity(Long.valueOf(batchnumList.get(i)));
//			pick.setCustomer(batchcustomer);
//			pick.setProjectName(batchprojectName);
			pick.setDescription(batchdescription);
			pick.setAccepter(accepter);
			this.pickUpApplicationService.saveOrUpdate(pick);
//			StockStatistics st = new StockStatistics();
//			st.setStock(stock);
//			st.setNum(Long.valueOf(batchnumList.get(i)));
//			st.setAccepter(accepter);
//			st.setPersonInCharge(batchpersonInCharge);
//			st.setProjectName(batchprojectName);
//			st.setCustomer(batchcustomer);
//			st.setDescription(batchdescription);
//			st.setInOrOut(false);
//			st.setOutboundOrder(orderNum);
//			BasicDataResult r = this.stockStatisticsService.inOrOutstockStatistics(st, user);
//			StockStatistics statics = (StockStatistics) r.getData();

//			List<PickUpApplication> pickUpApplication = this.pickUpApplicationService.findPickUpApplicationsByStockId(stock.getId());
//			long ycknum = pickUpApplication.stream().map(PickUpApplication::getEstimatedIssueQuantity).reduce((long) 0,Long::sum);
//			long acnum = pickUpApplication.stream().map(PickUpApplication::getActualIssueQuantity).reduce((long) 0,Long::sum);
//			
//			
//			StockStatistics s = new StockStatistics();
//			s.setId(statics.getStock().getId());
//			s.setNewNum(statics.getNewNum());
//			s.setRemainingNum(statics.getNewNum()-(ycknum-acnum));
//			list.add(s);
		}
		// 出库成功清除session
		session.removeAttribute(Contents.STOCK_LIST);

		return new BasicDataResult(200, "批量预出库成功!", list);
	}

	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/pickUpApplication/download")
	@SystemControllerLog(description = "下载预出库导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "批量预出库导入模版.xlsx";
		String contentType = "application/octet-stream";
		String UPLOAD = "Templates/";
		FileOperateUtil.download(request, response, storeName, contentType, UPLOAD);
		return null;
	}

	/***
	 * 文件上传
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/pickUpApplication/upload")
	@SystemControllerLog(description = "批量导入预出库")
	@RequiresPermissions(value = "pickUpApplication:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/pickUpApplications");
		String error = this.pickUpApplicationService.upload(request, session);
		attr.addFlashAttribute("errorImport", error);
		return modelAndView;

	}

	@RequestMapping(value = "/pickUpApplication/getbatch", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult getbatch(HttpSession session, String id) {

		if (Common.isNotEmpty(id)) {
			List ids = Arrays.asList(id.split(","));

			List<PickUpApplication> list = this.pickUpApplicationService.getbatchByids(ids);

			return new BasicDataResult(200, "获取预出库列表", list);
		} else {
			return new BasicDataResult(400, "获取预出库列表失败，请先选择预出库设备！", null);
		}

	}

	/**
	 * 跳转到预库存添加页面
	 */
	@RequestMapping(value = "/pickUpApplication/batchAdd", method = RequestMethod.POST)
//	@RequiresPermissions(value = "pickUpApplication:batch")
	@ResponseBody
	public BasicDataResult batchAdd(Model model, String batchid, String batchnum, HttpSession session) {

		String[] ids = batchid.split(",");
		String[] nums = batchnum.split(",");
		List<String> batchidList = Arrays.asList(ids);
		List<String> batchnumList = Arrays.asList(nums);

		if (batchidList.size() != batchnumList.size()) {
			return null;
		}
		StringBuilder errorMsg = new StringBuilder("");
		for (int i = 0; i < batchidList.size(); i++) {
			Integer num = Integer.valueOf(batchnumList.get(i));

			PickUpApplication getpickUpApplication = this.pickUpApplicationService.findOneById(batchidList.get(i),
					PickUpApplication.class);
			if (num <= 0) {
				 errorMsg.append("批量出库：" + getpickUpApplication.getStock().getName() + "出库数量有误<BR/>");
				 continue;
//				return new BasicDataResult().build(400, "出库数量有误！", "出库数量有误！");
			}

			long estimatedIssueQuantity = getpickUpApplication.getEstimatedIssueQuantity();// 预计出库数量
			long actualIssueQuantity = getpickUpApplication.getActualIssueQuantity();// 实际出库数量

			long newNum = estimatedIssueQuantity - actualIssueQuantity;
			if (num > newNum) {
				errorMsg.append("批量出库：" + getpickUpApplication.getStock().getName() + "出库数量不能超过剩余数量<BR/>");
				 continue;
				//return new BasicDataResult().build(400, "出库数量不能超过剩余数量！", "出库数量不能超过剩余数量");
			}

			int status = getpickUpApplication.getStatus();
			if (status != 1 && status != 3) {
				errorMsg.append("批量出库：" + getpickUpApplication.getStock().getName() + "设备可出库数量为0<BR/>");
				 continue;
				//return new BasicDataResult().build(400, "设备可出库数量为0", "设备可出库数量为0");
			}
			User suser = (User) session.getAttribute(Contents.USER_SESSION);
			getpickUpApplication.setHandler(suser);
			getpickUpApplication.setNum(num);
			BasicDataResult pickUpApplicationToStock = this.stockService.pickUpApplicationToStock(getpickUpApplication);

//			if (pickUpApplicationToStock.getStatus() == 200) {
//				// 出库成功 推送消息
//
//				// 创建通知
//				InventoryRole inventoryRole = this.inventoryRoleService.findByType("HANDLER");
//				if (Common.isEmpty(inventoryRole)) {
//					errorMsg.append("批量预出库成功，未获得绑定微信人员信息<BR/>");
//					//return new BasicDataResult().build(200, "出库成功", "未获得绑定微信人员信息");
//				}
//				List<User> users = inventoryRole.getUsers();
//
//				
//				Map<String, String> map = new HashMap<>();
//				map.put("first", "设备出库提醒！");
//				map.put("keyword1", getpickUpApplication.getStock().getName());
//				map.put("keyword2", String.valueOf(num));
//				map.put("keyword3", getpickUpApplication.getNewCustomer().getName());
//				map.put("keyword4", getpickUpApplication.getPname().getPm());
//				map.put("remark", "设备出库已完成");
//				users.stream().filter(user -> Common.isEmpty(user.getOpenId())).forEach(user -> {
//					errorMsg.append("用户：" + user.getUserName() + "尚未绑定微信<BR/>");
//				});
//				users.stream().filter(user -> Common.isNotEmpty(user.getOpenId())).forEach(user -> {
//					String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId3, user.getOpenId(), "", map);
//					if (sendWxMessage == "-1") {
//						errorMsg.append("用户：" + user.getUserName() + "消息发送失败！<BR/>");
//					}
//					// this.wxMsgPush.sendWxMessage(templateId1, "ooiMKv7cqR-2EgkeC9LdATpr-mbY",
//					// "www.baidu.com", map);
//				});
//			}
		}
		return new BasicDataResult().build(200, "出库成功", errorMsg);
	}
	
	
	
	
	
	
	@RequestMapping(value = "/pickUpApplication/batchEdit", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult batchPaymentOrderNo(HttpSession session,
											   @RequestParam(value = "stockid", defaultValue = "") String stockid,
											   @RequestParam(value = "description",required = false,defaultValue = "")String description
	) {
		List<String> array = Arrays.asList(stockid.split(","));
		for (String id : array) {
			PickUpApplication pickUpApplication = this.pickUpApplicationService.findOneById(id, PickUpApplication.class);
			
			
			PickUpApplication p = new PickUpApplication();
			BeanUtils.copyProperties(pickUpApplication, p);
			p.setDescription(description);
			this.pickUpApplicationService.saveOrUpdate(p);
		}
		
		return new BasicDataResult(200, "修改数据成功", "");

	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
