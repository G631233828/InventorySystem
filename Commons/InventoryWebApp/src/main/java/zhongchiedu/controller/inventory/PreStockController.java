package zhongchiedu.controller.inventory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
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

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.InventoryRole;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.PreStockServiceImpl;
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
public class PreStockController {

	private @Autowired PreStockServiceImpl preStockService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	private @Autowired StockService stockService;

	private @Autowired InventoryRoleService inventoryRoleService;

	private @Autowired WxMsgPush wxMsgPush;
	@Value("${templateId1}")
	private String templateId1;
	@Value("${templateId2}")
	private String templateId2;
	@Value("${qrcode.weburl}")
	private String weburl;

	@GetMapping("preStocks")
	@RequiresPermissions(value = "preStock:list")
	@SystemControllerLog(description = "查询所有预库存管理")
	public String prestock(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "status", defaultValue = "1") String status,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea,
			@ModelAttribute("errorMsg") String errorMsg) {

		Pagination<PreStock> pagination = this.preStockService.findpagination(pageNo, pageSize, search, searchArea,
				Integer.valueOf(status));
		model.addAttribute("pageList", pagination);

		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);

		session.setAttribute("prepageNo", pageNo);
		session.setAttribute("prepageSize", pageSize);
		session.setAttribute("presearch", search);
		session.setAttribute("presearchArea", searchArea);
		session.setAttribute("status", status);
		model.addAttribute("prepageSize", pageSize);
		model.addAttribute("presearch", search);
		model.addAttribute("presearchArea", searchArea);
		model.addAttribute("status", status);
		model.addAttribute("errorMsg", errorMsg);

		return "admin/preStock/list";
	}

	/**
	 * 跳转到预库存添加页面
	 */
	@GetMapping("/preStock")
	@RequiresPermissions(value = "preStock:add")
	public String addpreStockPage(Model model) {
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		return "admin/preStock/add";
	}

	/**
	 * 跳转到预库存添加页面
	 */
	@GetMapping("/preStock{id}")
	@RequiresPermissions(value = "preStock:edit")
	public String editEstimatePage(Model model, @PathVariable String id) {
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		PreStock stock = this.preStockService.findOneById(id, PreStock.class);
		model.addAttribute("stock", stock);
		if (Common.isNotEmpty(stock.getArea())) {
			// 所有货架
			List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false, stock.getArea().getId());
			model.addAttribute("goodsStorages", list);
		}

		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		return "admin/preStock/add";
	}

	/**
	 * 跳转到预库存添加页面
	 */
	@GetMapping("/preStockAdd{id}")
	@RequiresPermissions(value = "preStock:add")
	public String inStockPage(Model model, @PathVariable String id) {
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		PreStock stock = this.preStockService.findOneById(id, PreStock.class);
		model.addAttribute("stock", stock);
		if (Common.isNotEmpty(stock.getArea())) {
			// 所有货架
			List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false, stock.getArea().getId());
			model.addAttribute("goodsStorages", list);
		}
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		return "admin/preStock/preAdd";
	}

	
	@PostMapping("/preStock")
	@RequiresPermissions(value = "preStock:add")
	@SystemControllerLog(description = "添加预库存")
	public String addPreStock(@ModelAttribute("preStock") PreStock preStock, HttpSession session)

			throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		preStock.setPublisher(user);// 发布人
		this.preStockService.saveOrUpdate(preStock);
		Integer pageNo = (Integer) session.getAttribute("prepageNo");
		Integer pageSize = (Integer) session.getAttribute("prepageSize");
		String search = (String) session.getAttribute("presearch");
		String searchArea = (String) session.getAttribute("presearchArea");
		return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;

	}

	@PutMapping("/preStockAdd")
	@RequiresPermissions(value = "preStock:in")
	@SystemControllerLog(description = "预库存添加至库存")
	public String addPreStockAdd(@ModelAttribute("preStock") PreStock preStock, HttpSession session,RedirectAttributes attr)
			throws UnsupportedEncodingException {
		
		Integer pageNo = (Integer) session.getAttribute("prepageNo");
		Integer pageSize = (Integer) session.getAttribute("prepageSize");
		String search = (String) session.getAttribute("presearch");
		String searchArea = (String) session.getAttribute("presearchArea");
		String status = "2";
		
		//获取预入库设备状态
		PreStock getpreStock = this.preStockService.findOneById(preStock.getId(), PreStock.class);
		if(getpreStock.getStatus()!=1) {
			//已经处理完
			attr.addFlashAttribute("errorMsg", "当前订单已由他人处理，无需重复添加！");

			return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status;
		}
		
		User suser = (User) session.getAttribute(Contents.USER_SESSION);
		preStock.setHandler(suser);
		this.stockService.preStockToStock(preStock);
		
		
		//创建通知
		Set<User> users = this.inventoryRoleService.findAllUserInInventoryRole();
		StringBuilder errorMsg = new StringBuilder("");
		Map<String, String> map = new HashMap<>();
		map.put("first", "设备入库提醒！");
		map.put("keyword1", getpreStock.getName());
		String unit =Common.isNotEmpty(getpreStock.getUnit())?getpreStock.getUnit().getName():"个";
		map.put("keyword2", getpreStock.getActualReceiptQuantity()+unit);
		if(Common.isNotEmpty(getpreStock.getGoodsStorage())) {
			String area=Common.isNotEmpty(getpreStock.getArea().getName())?getpreStock.getArea().getName():"";
			String address =Common.isNotEmpty(getpreStock.getGoodsStorage().getAddress())? getpreStock.getGoodsStorage().getAddress():"";
			String shelfNumber=Common.isNotEmpty(getpreStock.getGoodsStorage().getShelfNumber())?getpreStock.getGoodsStorage().getShelfNumber():"";
			String shelflevel =Common.isNotEmpty(getpreStock.getGoodsStorage().getShelflevel())?"/"+getpreStock.getGoodsStorage().getShelflevel():"";

			map.put("keyword3", area+"仓库，地址：" + address + "存放在"+shelfNumber+shelflevel);
		}else {
			map.put("keyword3", "设备已经存放在:"+getpreStock.getArea().getName());
		}

		map.put("remark", "预计入库数量："+getpreStock.getEstimatedInventoryQuantity()+"\n实际入库:"+getpreStock.getActualReceiptQuantity()+"\n入库操作已完成!");
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
		
		return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status;

	}


	
		
		
	
	
	
	
	
	
	
	
	
	
	@PutMapping("/preStock")
	@RequiresPermissions(value = "preStock:edit")
	@SystemControllerLog(description = "修改预库存")
	public String editPreStock(@ModelAttribute("preStock") PreStock preStock, HttpSession session)
			throws UnsupportedEncodingException {
		this.preStockService.saveOrUpdate(preStock);
		Integer pageNo = (Integer) session.getAttribute("prepageNo");
		Integer pageSize = (Integer) session.getAttribute("prepageSize");
		String search = (String) session.getAttribute("presearch");
		String searchArea = (String) session.getAttribute("presearchArea");

		return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;

	}

	@DeleteMapping("/preStock/{id}")
	@RequiresPermissions(value = "preStock:delete")
	@SystemControllerLog(description = "删除预库存")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		Integer pageNo = (Integer) session.getAttribute("prepageNo");
		Integer pageSize = (Integer) session.getAttribute("prepageSize");
		String search = (String) session.getAttribute("presearch");
		String searchArea = (String) session.getAttribute("presearchArea");
		String status = (String) session.getAttribute("status");
		log.info("删除设备" + id);
		this.preStockService.delete(id);
		log.info("删除设备" + id + "成功");
		return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status;
	}

	@RequestMapping("/preStock/clearSearch")
//	@RequiresPermissions(value = "projectStock:delete")
	public String clearSearch(HttpSession session) {
		session.removeAttribute("pageNo");
		session.removeAttribute("pageSize");
		session.removeAttribute("search");
		session.removeAttribute("searchArea");
		return "redirect:/preStocks";

	}

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
		map.put("keyword2", preStock.getId());
		String model = Common.isNotEmpty(preStock.getModel()) ? "型号：" + preStock.getModel() : "";
		String estimatedWarehousingTime = Common.isNotEmpty(preStock.getEstimatedWarehousingTime())
				? "预计将在：" + preStock.getEstimatedWarehousingTime() + "到达"
				: "";
		map.put("keyword3", "采购的" + preStock.getName() + model + estimatedWarehousingTime + "请注意查收及入库。");
		map.put("remark", "点击此条信息可以通过手机进行入库操作！");
		users.stream().filter(user->Common.isEmpty(user.getOpenId())).forEach(user->{
			errorMsg.append("用户："+user.getUserName()+"尚未绑定微信<BR/>");
		});
		users.stream().filter(user->Common.isNotEmpty(user.getOpenId())).forEach(user->{
			String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId1, user.getOpenId(), weburl+"/wechat/preStockToStock/"+preStock.getId(), map);
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

public static void main(String[] args) {
	StringBuilder errorMsg = new StringBuilder();
	System.out.println(errorMsg.length()==0);
}

}