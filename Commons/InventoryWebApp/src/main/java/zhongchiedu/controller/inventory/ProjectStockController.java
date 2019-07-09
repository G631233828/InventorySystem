package zhongchiedu.controller.inventory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

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
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.ProjectStockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
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


	private @Autowired SupplierServiceImpl supplierService;


	@GetMapping("projectStocks")
	@RequiresPermissions(value = "projectStock:list")
	@SystemControllerLog(description = "查询所有项目库存管理")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "search", defaultValue = "") String search) {
		model.addAttribute("errorImport", errorImport);
		Pagination<ProjectStock> pagination = this.projectStockService.findpagination(pageNo, pageSize, search);
		model.addAttribute("pageList", pagination);
		List<String> listColums = this.columnService.findColumns("projectStock");
		model.addAttribute("listColums", listColums);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);

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

		return "admin/projectStock/add";
	}

	@PostMapping("/projectStock")
	@RequiresPermissions(value = "projectStock:add")
	@SystemControllerLog(description = "添加项目设备")
	public String addUser(
			@RequestParam(value="projectName",defaultValue="")String projectName,
			@RequestParam(value="name",defaultValue="")String name,
			@RequestParam(value="model",defaultValue="")String model,
			@RequestParam(value="scope",defaultValue="")String scope,
			@RequestParam(value="paymentTime",defaultValue="")String paymentTime,
			@RequestParam(value="supplier",defaultValue="")String supplier,
			@RequestParam(value="projectedProcurementVolume",defaultValue="0")long projectedProcurementVolume,
			@RequestParam(value="estimatedUnitPrice",defaultValue="0")long estimatedUnitPrice,
			@RequestParam(value="actualPurchaseQuantity",defaultValue="0")long actualPurchaseQuantity,
			@RequestParam(value="realCostUnitPrice",defaultValue="0")long realCostUnitPrice,
			@RequestParam(value="paymentAmount",defaultValue="0")long paymentAmount,
			@RequestParam(value="num",defaultValue="0")long num
			) {
		ProjectStock projectStock = new ProjectStock();
		projectStock.setProjectName(projectName);
		projectStock.setName(name);
		projectStock.setModel(model);
		projectStock.setScope(scope);
		projectStock.setPaymentTime(paymentTime);
		projectStock.setSupplier(this.supplierService.findOneById(supplier, Supplier.class));
		projectStock.setProjectedProcurementVolume(projectedProcurementVolume);
		projectStock.setEstimatedUnitPrice(estimatedUnitPrice);
		projectStock.setActualPurchaseQuantity(actualPurchaseQuantity);
		projectStock.setRealCostUnitPrice(realCostUnitPrice);
		projectStock.setPaymentAmount(paymentAmount);
		projectStock.setNum(num);
		
		this.projectStockService.saveOrUpdate(projectStock);
		return "redirect:projectStocks";
	}

	@PutMapping("/projectStock")
	@RequiresPermissions(value = "projectStock:edit")
	@SystemControllerLog(description = "修改项目设备")
	public String edit(
			@RequestParam(value="id",defaultValue="")String id,
			@RequestParam(value="projectName",defaultValue="")String projectName,
			@RequestParam(value="name",defaultValue="")String name,
			@RequestParam(value="model",defaultValue="")String model,
			@RequestParam(value="scope",defaultValue="")String scope,
			@RequestParam(value="paymentTime",defaultValue="")String paymentTime,
			@RequestParam(value="supplier",defaultValue="")String supplier,
			@RequestParam(value="projectedProcurementVolume",defaultValue="0")long projectedProcurementVolume,
			@RequestParam(value="estimatedUnitPrice",defaultValue="0")long estimatedUnitPrice,
			@RequestParam(value="actualPurchaseQuantity",defaultValue="0")long actualPurchaseQuantity,
			@RequestParam(value="realCostUnitPrice",defaultValue="0")long realCostUnitPrice,
			@RequestParam(value="paymentAmount",defaultValue="0")long paymentAmount,
			@RequestParam(value="num",defaultValue="0")long num
			) {
		ProjectStock projectStock = new ProjectStock();
		projectStock.setId(id);
		projectStock.setProjectName(projectName);
		projectStock.setName(name);
		projectStock.setModel(model);
		projectStock.setScope(scope);
		projectStock.setPaymentTime(paymentTime);
		projectStock.setSupplier(this.supplierService.findOneById(supplier, Supplier.class));
		projectStock.setProjectedProcurementVolume(projectedProcurementVolume);
		projectStock.setEstimatedUnitPrice(estimatedUnitPrice);
		projectStock.setActualPurchaseQuantity(actualPurchaseQuantity);
		projectStock.setRealCostUnitPrice(realCostUnitPrice);
		projectStock.setPaymentAmount(paymentAmount);
		projectStock.setNum(num);
		
		this.projectStockService.saveOrUpdate(projectStock);
		return "redirect:projectStocks";
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
		return "admin/projectStock/add";

	}

	@DeleteMapping("/projectStock/{id}")
	@RequiresPermissions(value = "projectStock:delete")
	@SystemControllerLog(description = "删除项目设备")
	public String delete(@PathVariable String id) {
		log.info("删除项目设备" + id);
		this.projectStockService.delete(id);
		log.info("删除项目设备" + id + "成功");
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
		return this.projectStockService.ajaxgetRepletes(projectName,name,model);
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
		modelAndView.setViewName("redirect:/stocks");
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
			@RequestParam(value = "flag", defaultValue = "") boolean flag) {
		return this.columnService.editColumns("projectStock", column, flag);
	}

	@RequestMapping(value = "/projectStock/getSupplier", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getSupplier(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.supplierService.findOneById(id);
	}

	/**
	 * 导出excel
	 */
	@RequestMapping(value="/projectStock/export")
	public void exportStock(HttpServletResponse response) {
			try{
				response.setContentType("application/vnd.ms-excel");
				String name = Common.fromDateYM()+"项目库存报表";
				String fileName = new String((name).getBytes("gb2312"), "ISO8859-1");
				HSSFWorkbook wb = this.projectStockService.export(name);
				response.setHeader("Content-disposition", "attachment;filename=" + fileName+".xls");
				OutputStream ouputStream = response.getOutputStream();
				wb.write(ouputStream);
				ouputStream.flush();
				ouputStream.close();
			}catch(IOException e){
				e.printStackTrace();
			}
			
	}

}
