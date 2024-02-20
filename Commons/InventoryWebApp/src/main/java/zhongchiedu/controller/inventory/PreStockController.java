package zhongchiedu.controller.inventory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.*;
import zhongchiedu.inventory.service.Impl.*;
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.inventory.service.StockService;
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

	private @Autowired SystemClassificationServiceImpl  ssCService;

	private @Autowired WxMsgPush wxMsgPush;

	private @Autowired ColumnServiceImpl columnService;
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
//			@RequestParam(value = "searchArea", defaultValue = "") String searchArea,
//			@RequestParam(value = "ssC", defaultValue = "") String ssC,
			@ModelAttribute RequestBo requestBo,
			@ModelAttribute("errorMsg") String errorMsg) throws JsonProcessingException {

		ObjectMapper objectMapper=new ObjectMapper();
		String jsonString=objectMapper.writeValueAsString(requestBo);
		model.addAttribute("Bo",jsonString);
		model.addAttribute("requestBo",requestBo);
//		Pagination<PreStock> pagination = this.preStockService.findpagination(pageNo, pageSize, search, searchArea,
//				Integer.valueOf(status),ssC);
		Pagination<PreStock> pagination = this.preStockService.findpagination(pageNo, pageSize, requestBo,Integer.valueOf(status));
		model.addAttribute("pageList", pagination);
		List<SystemClassification>  ssCs=this.ssCService.findAllSystemClassification(false);
		model.addAttribute("ssCs",ssCs);
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		List<String> listColums = this.columnService.findColumns("prestock",user.getId());

		model.addAttribute("listColums", listColums);
		session.setAttribute("prepageNo", pageNo);
		session.setAttribute("prepageSize", pageSize);
		session.setAttribute("presearch", search);
//		session.setAttribute("presearchArea", searchArea);
		session.setAttribute("status", status);
//		session.setAttribute("ssC",ssC);
		model.addAttribute("prepageSize", pageSize);
		model.addAttribute("presearch", search);
//		model.addAttribute("presearchArea", searchArea);
		model.addAttribute("status", status);
		model.addAttribute("errorMsg", errorMsg);
//		model.addAttribute("ssC",ssC);
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

		List<SystemClassification>  ssCs=this.ssCService.findAllSystemClassification(false);
		model.addAttribute("ssCs",ssCs);
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

		List<SystemClassification>  ssCs=this.ssCService.findAllSystemClassification(false);
		model.addAttribute("ssCs",ssCs);
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

		List<SystemClassification>  ssCs=this.ssCService.findAllSystemClassification(false);
		model.addAttribute("ssCs",ssCs);
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
		String ssC = (String) session.getAttribute("ssC");

		return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea + "&ssC=" +ssC;

	}

	@PutMapping("/preStockAdd")
	@RequiresPermissions(value = "preStock:in")
	@SystemControllerLog(description = "预库存添加至库存")
	public String addPreStockAdd(@ModelAttribute("preStock") PreStock preStock, @ModelAttribute("num")Long num, HttpSession session,RedirectAttributes attr)
			throws UnsupportedEncodingException {
		
		Integer pageNo = (Integer) session.getAttribute("prepageNo");
		Integer pageSize = (Integer) session.getAttribute("prepageSize");
		String search = (String) session.getAttribute("presearch");
		String searchArea = (String) session.getAttribute("presearchArea");
		String status = "1";
		String ssC = (String) session.getAttribute("ssC");

		//获取预入库设备状态
		PreStock getpreStock = this.preStockService.findOneById(preStock.getId(), PreStock.class);
		if(getpreStock.getStatus()!=1) {
			//已经处理完
			attr.addFlashAttribute("errorMsg", "当前订单已由他人处理，无需重复添加！");

			return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status + "&ssC=" +ssC;
		}
		if(num<=0 || Common.isEmpty(num)){
			//入库数量有问题
			attr.addFlashAttribute("errorMsg", "入库数量有问题");

			return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status + "&ssC=" +ssC;
		}

		User suser = (User) session.getAttribute(Contents.USER_SESSION);
		preStock.setHandler(suser);
		preStock.setActualReceiptQuantity(num);
		this.stockService.preStockToStock(preStock,getpreStock.getActualReceiptQuantity());
//		this.stockService.preStockToStock(preStock,num);
		
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

		map.put("remark", "预计入库数量："+getpreStock.getEstimatedInventoryQuantity()+"\n实际入库:"+preStock.getActualReceiptQuantity()+"\n入库操作已完成!");
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
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status + "&ssC=" +ssC;

	}


	
		
		
	
	
	
	
	
	
	
	
	
	
	@PutMapping("/preStock")
	@RequiresPermissions(value = "preStock:edit")
	@SystemControllerLog(description = "修改预库存")
	public String editPreStock(@ModelAttribute("preStock") PreStock preStock, HttpSession session)
			throws UnsupportedEncodingException {
		this.preStockService.saveOrUpdate(preStock);


		return "redirect:/preStocks";

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
		String ssC = (String) session.getAttribute("ssC");

		return "redirect:/preStocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea+ "&status=" + status + "&ssC=" +ssC;
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


	/**
	 * 模版下载
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/prestock/download")
	@SystemControllerLog(description = "下载库存管理导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "预库存管理模版.xlsx";
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
	@RequestMapping(value = "/prestock/upload")
	@SystemControllerLog(description = "批量导入预库存管理")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/preStocks");
		String error = this.preStockService.upload(request, session);
		attr.addFlashAttribute("errorMsg", error);
		return modelAndView;

	}

	/**
	 * 导出excel
	 */
//	@RequestMapping(value = "/prestock/export")
//	public void exportStock(HttpServletResponse response,
//							@RequestParam(value = "areaId", defaultValue = "") String areaId) {
//		try {
//			response.setContentType("application/vnd.ms-excel");
//			String name = Common.fromDateYM() + "预库存报表";
//			String fileName = new String((name).getBytes("gb2312"), "ISO8859-1");
//			HSSFWorkbook wb = this.preStockService.export(name, areaId);
//			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
//			OutputStream ouputStream = response.getOutputStream();
//			wb.write(ouputStream);
//			ouputStream.flush();
//			ouputStream.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//	}

	@RequestMapping(value = "/prestock/export")
	public void exportpreStock(HttpServletResponse response,HttpServletRequest request,
							@ModelAttribute RequestBo requestBo)  throws Exception{
		String exportName = Common.fromDateYMD() + "预库存报表";

		response.setContentType("application/vnd.ms-excel");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		Workbook newExport = this.preStockService.newExport(request, requestBo);

		OutputStream out = response.getOutputStream();
		newExport.write(out);
		out.flush();
		out.close();

	}


	@RequestMapping(value = "/prestock/getItems", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult getItems(HttpSession session,String id) {
//		List list = (List) session.getAttribute(Contents.STOCK_LIST);
//		if(list==null&& Common.isEmpty(id)) {
//			return new BasicDataResult(400, "获取出库列表失败，请先添加或选中出库商品！", null);
//		}
//		if(Common.isNotEmpty(id)) {
//			list = Arrays.asList(id.split(","));
//			list.forEach(o->{
//				List getstockSession = (List) session.getAttribute(Contents.STOCK_LIST);
//
//				if(getstockSession==null) {
//					List listid = new ArrayList<>();
//					listid.add(o);
//					session.setAttribute(Contents.STOCK_LIST, listid);
//				}else {
//					if(!getstockSession.contains(id)) {
//						getstockSession.add(o);
//						session.setAttribute(Contents.STOCK_LIST, getstockSession);
//					}
//
//				}
//			});
//
//
//
//		}

		List list=new ArrayList<>();

		if(Common.isNotEmpty(id)) {
			list = Arrays.asList(id.split(","));

		}

		if(Common.isNotEmpty(list)&&list.size()>0) {
			List<PreStock> stocks = this.preStockService.findStocksByIds(list);
			List<PreStock> liststock = new ArrayList<>();
			//便利 数据过滤
			stocks.forEach(s->{
				PreStock stock = new PreStock();
				stock.setName(s.getName());
				stock.setActualReceiptQuantity(s.getEstimatedInventoryQuantity()-s.getActualReceiptQuantity());//用actualReceiptQuantity代替库存量
				stock.setModel(s.getModel());
				stock.setId(s.getId());
				liststock.add(stock);
			});
			return new BasicDataResult(200, "获取入库列表", liststock);
		}else {
			return new BasicDataResult(400, "获取入库列表失败，请先添加入库商品！", null);
		}

	}

	@RequestMapping(value = "/prestock/checkNum", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult checkNum(HttpSession session,
									@RequestParam(value = "id", defaultValue = "") String id,
									@RequestParam(value = "num", defaultValue = "") String num) {
		if(Common.isNotEmpty(id)) {

			boolean isnum = StringUtils.isNumeric(num);
			if(!isnum) {
				return new BasicDataResult(400, "请输入合法的数字！", "");
			}
			//根据id获取库存商品
			PreStock stock = this.preStockService.findOneById(id, PreStock.class);
			Long eaqu=stock.getEstimatedInventoryQuantity()-stock.getActualReceiptQuantity();
			if(eaqu<Long.valueOf(num)) {
				return new BasicDataResult(400, "入库数量大于库存数量，请检查！", "");
			}
			return new BasicDataResult(200, "库存无误！", "");
		}

		return new BasicDataResult(400, "获取库存信息失败", "");
	}

	@RequestMapping(value = "/prestock/batchOut", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "批量入库")
	public BasicDataResult batchOut(String batchid, String batchnum, HttpSession session) {
		String[] ids = batchid.split(",");
		String[] nums = batchnum.split(",");
		List<String> batchidList = Arrays.asList(ids);
		List<String> batchnumList = Arrays.asList(nums);
		if (batchidList.size() != batchnumList.size()) {
			return new BasicDataResult(400, "出库商品与id不匹配", "");
		}
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < batchidList.size(); i++) {
			PreStock preStock=this.preStockService.findOneById(batchidList.get(i),PreStock.class);
			PreStock stock=new PreStock();
			stock.setId(preStock.getId());
			stock.setEstimatedInventoryQuantity(preStock.getEstimatedInventoryQuantity());
			Long actnum=preStock.getActualReceiptQuantity();//之前入库的数量
			stock.setActualReceiptQuantity(actnum+Long.valueOf(batchnumList.get(i)));
			preStock.setActualReceiptQuantity(Long.valueOf(batchnumList.get(i)));
			this.stockService.preStockToStock(preStock,actnum);
			list.add(stock);
		}


		return new BasicDataResult(200, "批量出库成功!", list);
	}



	@RequestMapping(value = "/prestock/batchEditprestock", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult batchPaymentOrderNo(HttpSession session,
											   @RequestParam(value = "stockid", defaultValue = "") String stockid,
											   @RequestParam(value = "inprice", defaultValue = "null") String inprice,
											   @RequestParam(value = "purchaseInvoiceNo", defaultValue = "null") String purchaseInvoiceNo,
											   @RequestParam(value = "paymentOrderNo", defaultValue = "null") String paymentOrderNo,
											   @RequestParam(value = "purchaseInvoiceDate", defaultValue = "null") String purchaseInvoiceDate,
											   @RequestParam(value = "itemNo", defaultValue = "null") String itemNo
	) {

		Double dinprice=null;
		Double dsailPrice=null;
		try {
			if(!inprice.equals("null")) {
				dinprice = Double.parseDouble(inprice);
			}else {
				dinprice=null;
			}
		}catch(Exception e) {
			e.printStackTrace();
			return new BasicDataResult(400, "入库金额输入有误，请输入正确的数字", "");

		}

		User user = (User) session.getAttribute(Contents.USER_SESSION);
		this.preStockService.updateStockStatistics(stockid, dinprice, purchaseInvoiceNo,purchaseInvoiceDate,paymentOrderNo,itemNo);
		return new BasicDataResult(200, "修改统计数据成功", "");

	}

	@RequestMapping(value = "/prestock/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
									   @RequestParam(value = "flag", defaultValue = "") boolean flag,HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		return this.columnService.editColumns("prestock", column, flag,user.getId());
	}

//	@RequestMapping(value = "/prestock/batchEditpreStocks", method = RequestMethod.POST)
//	@ResponseBody
//	public BasicDataResult batchPaymentOrderNo(HttpSession session,
//											   @RequestParam(value = "stockid", defaultValue = "") String stockid,
//											   @RequestParam(value = "inprice", defaultValue = "null") String inprice,
//											   @RequestParam(value = "purchaseInvoiceNo", defaultValue = "null") String purchaseInvoiceNo,
//											   @RequestParam(value = "receiptNo", defaultValue = "null") String receiptNo,
//											   @RequestParam(value = "paymentOrderNo", defaultValue = "null") String paymentOrderNo,
//											   @RequestParam(value = "sailesInvoiceNo", defaultValue = "null") String sailesInvoiceNo,
//											   @RequestParam(value = "sailesInvoiceDate", defaultValue = "null") String sailesInvoiceDate,
//											   @RequestParam(value = "purchaseInvoiceDate", defaultValue = "null") String purchaseInvoiceDate,
//											   @RequestParam(value = "sailPrice", defaultValue = "null") String sailPrice,
//											   @RequestParam(value = "itemNo", defaultValue = "null") String itemNo,
//											   @RequestParam(value = "description", defaultValue = "null") String description
//	) {
//
//		Double dinprice=null;
//		Double dsailPrice=null;
//		try {
//			if(!inprice.equals("null")) {
//				dinprice = Double.parseDouble(inprice);
//			}else {
//				dinprice=null;
//			}
//		}catch(Exception e) {
//			e.printStackTrace();
//			return new BasicDataResult(400, "入库金额输入有误，请输入正确的数字", "");
//
//		}
//
//		try {
//			if(!sailPrice.equals("null")) {
//				dsailPrice = Double.parseDouble(sailPrice);
//			}else {
//				dsailPrice=null;
//			}
//		}catch(Exception e) {
//			e.printStackTrace();
//			return new BasicDataResult(400, "销售金额输入有误，请输入正确的数字", "");
//
//		}
//		User user = (User) session.getAttribute(Contents.USER_SESSION);
//		this.preStockService.updateStockStatistics(stockid, dinprice, purchaseInvoiceNo, purchaseInvoiceDate,paymentOrderNo,itemNo);
//
//		return new BasicDataResult(200, "修改统计数据成功", "");
//
//	}

public static void main(String[] args) {
	StringBuilder errorMsg = new StringBuilder();
	System.out.println(errorMsg.length()==0);
}

}
