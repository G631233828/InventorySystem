package zhongchiedu.controller.inventory;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.service.Impl.BrandServiceImpl;
import zhongchiedu.inventory.service.Impl.CategoryServiceImpl;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.SystemClassificationServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 供应商
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class SupplierController {

	
	private @Autowired SupplierServiceImpl supplierService;

	
	private @Autowired CategoryServiceImpl categoryServiceImpl;
	
	
	private @Autowired SystemClassificationServiceImpl systemClassificationService;
	
	private @Autowired BrandServiceImpl brandService;

	private @Autowired ColumnServiceImpl columnService;
	
	
	
	
	
	@GetMapping("suppliers")
	@RequiresPermissions(value = "supplier:list")
	@SystemControllerLog(description = "查询所有供应商信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
		model.addAttribute("errorImport", errorImport);
		Pagination<Supplier> pagination = this.supplierService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		List<String> listColums = this.columnService.findColumns("supplier");
		model.addAttribute("listColums",listColums);
		
		return "admin/supplier/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/supplier")
	@RequiresPermissions(value = "supplier:add")
	public String addPage(Model model) {
		List<Category> list = this.categoryServiceImpl.findAllCategory(false);
		model.addAttribute("categorys", list);
		
		//所有系统分类
		List<SystemClassification> syslist = this.systemClassificationService.findAllSystemClassification(false);
		model.addAttribute("systemClassifications", syslist);
		//所有品牌
		List<Brand> brandlist = this.brandService.findAllBrand(false);
		model.addAttribute("brands", brandlist);
		
		return "admin/supplier/add";
	}

	@PostMapping("/supplier")
	@RequiresPermissions(value = "supplier:add")
	@SystemControllerLog(description = "添加供应商")
	public String addUser(@ModelAttribute("supplier") Supplier supplier,String types) {
		this.supplierService.saveOrUpdate(supplier,types);
		return "redirect:suppliers";
	}

	@PutMapping("/supplier")
	@RequiresPermissions(value = "supplier:edit")
	@SystemControllerLog(description = "修改供应商")
	public String edit(@ModelAttribute("supplier") Supplier supplier,String types) {
		this.supplierService.saveOrUpdate(supplier,types);
		return "redirect:suppliers";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/supplier{id}")
	@RequiresPermissions(value = "supplier:edit")
	@SystemControllerLog(description = "编辑供应商")
	public String toeditPage(@PathVariable String id, Model model) {
		Supplier supplier = this.supplierService.findOneById(id,
				Supplier.class);
		model.addAttribute("supplier", supplier);
		Object[] selectcategorys = this.supplierService.categorys(supplier);
		model.addAttribute("selectcategorys", selectcategorys);
		List<Category> list = this.categoryServiceImpl.findAllCategory(false);
		model.addAttribute("categorys", list);
		//所有系统分类
		List<SystemClassification> syslist = this.systemClassificationService.findAllSystemClassification(false);
		model.addAttribute("systemClassifications", syslist);
		//所有品牌
		List<Brand> brandlist = this.brandService.findAllBrand(false);
		model.addAttribute("brands", brandlist);
		
		return "admin/supplier/add";

	}

	@DeleteMapping("/supplier/{id}")
	@RequiresPermissions(value = "supplier:delete")
	@SystemControllerLog(description = "删除供应商")
	public String delete(@PathVariable String id) {
		log.info("删除供应商" + id);
		this.supplierService.delete(id);
		log.info("删除供应商" + id + "成功");
		return "redirect:/suppliers";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/supplier/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name) {
		return this.supplierService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/supplier/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.supplierService.todisable(id);
	}

	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/supplier/download")
	@SystemControllerLog(description = "下载供应商信息导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "供应商信息模版.xlsx";
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
	@RequestMapping(value = "/supplier/upload")
	@SystemControllerLog(description = "批量导入供应商信息")
	@RequiresPermissions(value = "supplier:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/suppliers");
		String error = this.supplierService.upload(request, session);
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
	@RequestMapping(value = "/supplier/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.supplierService.findproInfo(request);
	}
	
	
	
	@RequestMapping(value = "/supplier/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column) {
		//TODO请求没有提交过来明天debug一下
		return this.columnService.editColumns("supplier", column);
	}
	
	
	
	
	
	
	
	
	
	
	
	

}
