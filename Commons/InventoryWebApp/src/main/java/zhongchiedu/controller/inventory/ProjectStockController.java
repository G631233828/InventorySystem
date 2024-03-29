package zhongchiedu.controller.inventory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
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
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.ProjectStockServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 项目设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class ProjectStockController {

	private @Autowired ProjectStockServiceImpl projectStockService;

	private @Autowired ColumnServiceImpl columnService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	@GetMapping("projectStocks")
	@RequiresPermissions(value = "projectStock:list")
	@SystemControllerLog(description = "查询所有项目库存管理")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "projectName", defaultValue = "") String projectName,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea) {
		model.addAttribute("errorImport", errorImport);
		
	
		
		Pagination<ProjectStock> pagination = this.projectStockService.findpagination(pageNo, pageSize, search,
				projectName, searchArea);
		model.addAttribute("pageList", pagination);
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		List<String> listColums = this.columnService.findColumns("projectStock",user.getId());
		
		Set set = this.projectStockService.findProjectNames();
		

		session.setAttribute("pageNo", pageNo);
		session.setAttribute("pageSize", pageSize);
		session.setAttribute("search", search);
		session.setAttribute("selectProjectName", projectName);
		session.setAttribute("searchArea", searchArea);
		
		model.addAttribute("listColums", listColums);
		model.addAttribute("projectName", set);
//		
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);

		return "admin/projectStock/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("projectStock")
	@RequiresPermissions(value = "projectStock:add")
	public String addPage(Model model) {
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);

		return "admin/projectStock/add";
	}

	@PostMapping("/projectStock")
	@RequiresPermissions(value = "projectStock:add")
	@SystemControllerLog(description = "添加项目设备")
	public String addUser(@RequestParam(value = "projectName", defaultValue = "") String projectName,
			@RequestParam(value = "name", defaultValue = "") String name,
			@RequestParam(value = "model", defaultValue = "") String model,
			@RequestParam(value = "scope", defaultValue = "") String scope,
			@RequestParam(value = "paymentTime", defaultValue = "") String paymentTime,
			@RequestParam(value = "supplier", defaultValue = "") String supplier,
			@RequestParam(value = "projectedProcurementVolume", defaultValue = "0") String projectedProcurementVolume,
			@RequestParam(value = "estimatedUnitPrice", defaultValue = "0") String estimatedUnitPrice,
			@RequestParam(value = "actualPurchaseQuantity", defaultValue = "0") String actualPurchaseQuantity,
			@RequestParam(value = "realCostUnitPrice", defaultValue = "0") String realCostUnitPrice,
			@RequestParam(value = "paymentAmount", defaultValue = "0") String paymentAmount,
			@RequestParam(value = "num", defaultValue = "0") String num,
			@RequestParam(value = "areaId", defaultValue = "") String areaId,HttpSession session
			) throws UnsupportedEncodingException {
		ProjectStock projectStock = new ProjectStock();
		projectStock.setProjectName(projectName);
		projectStock.setName(name);
		projectStock.setModel(model);
		projectStock.setScope(scope);
		projectStock.setPaymentTime(paymentTime);
		projectStock.setSupplier(this.supplierService.findOneById(supplier, Supplier.class));
		projectStock.setProjectedProcurementVolume(Integer.valueOf(projectedProcurementVolume));
		projectStock.setEstimatedUnitPrice(Double.valueOf(estimatedUnitPrice));
		projectStock.setActualPurchaseQuantity(Integer.valueOf(actualPurchaseQuantity));
		projectStock.setRealCostUnitPrice(Double.valueOf(realCostUnitPrice));
		projectStock.setPaymentAmount(Double.valueOf(paymentAmount));
		projectStock.setInventory(Integer.valueOf(actualPurchaseQuantity));
		projectStock.setNum(Integer.valueOf(num));

		this.projectStockService.saveOrUpdate(projectStock,areaId);
		
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		String search =  (String)session.getAttribute("search");
		String searchArea = (String)session.getAttribute("searchArea");
		String selectProjectName = (String)session.getAttribute("selectProjectName");
		return "redirect:/projectStocks?pageNo="+pageNo+"&projectName="+URLEncoder.encode(selectProjectName,"UTF-8") +"&pageSize="+pageSize+"&search="+URLEncoder.encode(search,"UTF-8")+"&searchArea="+searchArea;
		
	}

	@PutMapping("/projectStock")
	@RequiresPermissions(value = "projectStock:edit")
	@SystemControllerLog(description = "修改项目设备")
	public String edit(@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "projectName", defaultValue = "") String projectName,
			@RequestParam(value = "name", defaultValue = "") String name,
			@RequestParam(value = "model", defaultValue = "") String model,
			@RequestParam(value = "scope", defaultValue = "") String scope,
			@RequestParam(value = "paymentTime", defaultValue = "") String paymentTime,
			@RequestParam(value = "supplier", defaultValue = "") String supplier,
			@RequestParam(value = "projectedProcurementVolume", defaultValue = "0") String projectedProcurementVolume,
			@RequestParam(value = "estimatedUnitPrice", defaultValue = "0") String estimatedUnitPrice,
			@RequestParam(value = "actualPurchaseQuantity", defaultValue = "0") String actualPurchaseQuantity,
			@RequestParam(value = "realCostUnitPrice", defaultValue = "0") String realCostUnitPrice,
			@RequestParam(value = "paymentAmount", defaultValue = "0") String paymentAmount,
			@RequestParam(value = "num", defaultValue = "0") String num,
			@RequestParam(value = "areaId", defaultValue = "") String areaId,HttpSession session
			) throws UnsupportedEncodingException {
		ProjectStock projectStock = new ProjectStock();
		projectStock.setId(id);
		projectStock.setProjectName(projectName);
		projectStock.setName(name);
		projectStock.setModel(model);
		projectStock.setScope(scope);
		projectStock.setPaymentTime(paymentTime);
		projectStock.setSupplier(this.supplierService.findOneById(supplier, Supplier.class));
		projectStock.setProjectedProcurementVolume(Integer.valueOf(projectedProcurementVolume));
		projectStock.setEstimatedUnitPrice(Double.valueOf(estimatedUnitPrice));
		projectStock.setActualPurchaseQuantity(Integer.valueOf(actualPurchaseQuantity));
		projectStock.setRealCostUnitPrice(Double.valueOf(realCostUnitPrice));
		projectStock.setPaymentAmount(Double.valueOf(paymentAmount));
		projectStock.setNum(Integer.valueOf(num));

		this.projectStockService.saveOrUpdate(projectStock,areaId);
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		String search =  (String)session.getAttribute("search");
		String searchArea = (String)session.getAttribute("searchArea");
		String selectProjectName = (String)session.getAttribute("selectProjectName");
		return "redirect:/projectStocks?pageNo="+pageNo+"&projectName="+URLEncoder.encode(selectProjectName,"UTF-8") +"&pageSize="+pageSize+"&search="+URLEncoder.encode(search,"UTF-8")+"&searchArea="+searchArea;
		
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/projectStock{id}")
	@RequiresPermissions(value = "projectStock:edit")
	@SystemControllerLog(description = "编辑项目设备")
	public String toeditPage(@PathVariable String id, Model model) {
		ProjectStock projectStock = this.projectStockService.findOneById(id, ProjectStock.class);
		model.addAttribute("projectStock", projectStock);
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		return "admin/projectStock/add";

	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/projectStockToStock{id}")
	@RequiresPermissions(value = "projectStock:edit")
	@SystemControllerLog(description = "编辑项目设备")
	public String projectStockToStock(@PathVariable String id, Model model) {
		ProjectStock projectStock = this.projectStockService.findOneById(id, ProjectStock.class);
		Stock stock = new Stock();
		stock.setName(projectStock.getName());
		stock.setModel(projectStock.getModel());
		stock.setScope(projectStock.getScope());
		stock.setSupplier(projectStock.getSupplier());
		model.addAttribute("stock", stock);

		// 所有货架
		List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false,"");
		model.addAttribute("goodsStorages", list);
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);

		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		return "admin/stock/add";

	}

	@DeleteMapping("/projectStock/{id}")
	@RequiresPermissions(value = "projectStock:delete")
	@SystemControllerLog(description = "删除项目设备")
	public String delete(@PathVariable String id,HttpSession session) throws UnsupportedEncodingException {
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search =  (String)session.getAttribute("search");
		String searchArea = (String)session.getAttribute("searchArea");
		String selectProjectName = (String)session.getAttribute("selectProjectName");
		
		log.info("删除项目设备" + id);
		this.projectStockService.delete(id);
		log.info("删除项目设备" + id + "成功");
		
		return "redirect:/projectStocks?pageNo="+pageNo+"&projectName="+URLEncoder.encode(selectProjectName,"UTF-8") +"&pageSize="+pageSize+"&search="+URLEncoder.encode(search,"UTF-8")+"&searchArea="+searchArea;
		
	}
	@RequestMapping("/projectStock/clearSearch")
//	@RequiresPermissions(value = "projectStock:delete")
	public String clearSearch(HttpSession session){
		session.removeAttribute("pageNo");
		session.removeAttribute("pageSize");
		session.removeAttribute("search");
		session.removeAttribute("searchArea");
		session.removeAttribute("selectProjectName");
		return "redirect:/projectStocks";
		
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/projectStock/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name,
			@RequestParam(value = "projectName", defaultValue = "") String projectName,
			@RequestParam(value = "model", defaultValue = "") String model) {
		return this.projectStockService.ajaxgetRepletes(projectName, name, model);
	}

	@RequestMapping(value = "/projectStock/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.projectStockService.todisable(id);
	}

	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/projectStock/download")
	@SystemControllerLog(description = "下载项目库存管理导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "项目库存管理模版.xlsx";
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
	@RequestMapping(value = "/projectStock/upload")
	@SystemControllerLog(description = "批量导入项目库存管理")
	@RequiresPermissions(value = "projectStock:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/projectStocks");
		String error = this.projectStockService.upload(request, session);
		attr.addFlashAttribute("errorImport", error);
		return modelAndView;

	}

	/**
	 * process 获取进度
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/projectStock/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.projectStockService.findproInfo(request);
	}

	@RequestMapping(value = "/projectStock/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag,HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		return this.columnService.editColumns("projectStock", column, flag,user.getId());
	}

	@RequestMapping(value = "/projectStock/getSupplier", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getSupplier(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.supplierService.findOneById(id);
	}

	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/projectStock/export")
	public void exportStock(HttpServletResponse response,@RequestParam(value = "areaId", defaultValue = "") String areaId) {
		try {
			response.setContentType("application/vnd.ms-excel");
			String name = Common.fromDateYM() + "项目库存报表";
			String fileName = new String((name).getBytes("gb2312"), "ISO8859-1");
			HSSFWorkbook wb = this.projectStockService.export(name,areaId);
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
			OutputStream ouputStream = response.getOutputStream();
			wb.write(ouputStream);
			ouputStream.flush();
			ouputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	
	
	

}
