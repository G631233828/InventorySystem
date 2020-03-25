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
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class StockController {

	private @Autowired StockServiceImpl stockService;

	private @Autowired ColumnServiceImpl columnService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	@GetMapping("stocks")
	@RequiresPermissions(value = "stock:list")
	@SystemControllerLog(description = "查询所有库存管理")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea) {
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		model.addAttribute("errorImport", errorImport);
		Pagination<Stock> pagination = this.stockService.findpagination(pageNo, pageSize, search, searchArea);
		model.addAttribute("pageList", pagination);
		List<String> listColums = this.columnService.findColumns("stock");
		model.addAttribute("listColums", listColums);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);
		model.addAttribute("searchArea", searchArea);

		return "admin/stock/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/stock")
	@RequiresPermissions(value = "stock:add")
	public String addPage(Model model) {
//		// 所有货架
//		List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false);
//		model.addAttribute("goodsStorages", list);
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);

		return "admin/stock/add";
	}

	@PostMapping("/stock")
	@RequiresPermissions(value = "stock:add")
	@SystemControllerLog(description = "添加设备")
	public String addUser(@ModelAttribute("stock") Stock stock) {
		this.stockService.saveOrUpdate(stock);
		return "redirect:stocks";
	}

	@PutMapping("/stock")
	@RequiresPermissions(value = "stock:edit")
	@SystemControllerLog(description = "修改设备")
	public String edit(@ModelAttribute("stock") Stock stock) {
		this.stockService.saveOrUpdate(stock);
		return "redirect:stocks";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/stock{id}")
	@RequiresPermissions(value = "stock:edit")
	@SystemControllerLog(description = "编辑设备")
	public String toeditPage(@PathVariable String id, Model model) {
		Stock stock = this.stockService.findOneById(id, Stock.class);
		model.addAttribute("stock", stock);
		
		// 所有货架
		List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false,stock.getArea().getId());
		model.addAttribute("goodsStorages", list);
		
		
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		return "admin/stock/add";

	}

	@DeleteMapping("/stock/{id}")
	@RequiresPermissions(value = "stock:delete")
	@SystemControllerLog(description = "删除设备")
	public String delete(@PathVariable String id) {
		log.info("删除设备" + id);
		this.stockService.delete(id);
		log.info("删除设备" + id + "成功");
		return "redirect:/stocks";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/stock/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name,@RequestParam(value = "areaId", defaultValue = "") String areaId,@RequestParam(value = "model", defaultValue = "") String model) {
		return this.stockService.ajaxgetRepletes(name,areaId,model);
	}

	
	
	/**
	 * 根据选择的目录获取菜单
	 * @param parentId
	 * @return
	 */
	@RequestMapping(value="/getStorages",method=RequestMethod.POST,produces="application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getparentmenu(@RequestParam(value = "areaId", defaultValue = "") String areaId){

		List<GoodsStorage> list = this.goodsStorageService.findStorages(areaId);
		
		return list!=null?BasicDataResult.build(200, "success",list):BasicDataResult.build(400, "error",null);
	}
	
	
	
	
	@RequestMapping(value = "/stock/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.stockService.todisable(id);
	}

	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/stock/download")
	@SystemControllerLog(description = "下载库存管理导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "库存管理模版.xlsx";
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
	@RequestMapping(value = "/stock/upload")
	@SystemControllerLog(description = "批量导入库存管理")
	@RequiresPermissions(value = "stock:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/stocks");
		String error = this.stockService.upload(request, session);
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
	@RequestMapping(value = "/stock/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.stockService.findproInfo(request);
	}

	@RequestMapping(value = "/stock/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag) {
		return this.columnService.editColumns("stock", column, flag);
	}

	@RequestMapping(value = "/stock/getSupplier", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getSupplier(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.supplierService.findOneById(id);
	}

	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/stock/export")
	public void exportStock(HttpServletResponse response,@RequestParam(value = "areaId", defaultValue = "") String areaId) {
		try {
			response.setContentType("application/vnd.ms-excel");
			String name = Common.fromDateYM() + "库存报表";
			String fileName = new String((name).getBytes("gb2312"), "ISO8859-1");
			HSSFWorkbook wb = this.stockService.export(name,areaId);
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
