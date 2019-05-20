package zhongchiedu.controller.inventory;

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
import zhongchiedu.inventory.service.Impl.BrandServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class BrandController {

	@Autowired
	private BrandServiceImpl brandService;


	@GetMapping("brands")
	@RequiresPermissions(value = "brand:list")
	@SystemControllerLog(description = "查询所有品牌信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
			model.addAttribute("errorImport", errorImport);
		Pagination<Brand> pagination = this.brandService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		return "admin/brand/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/brand")
	@RequiresPermissions(value = "brand:add")
	public String addPage(Model model) {
		return "admin/brand/add";
	}

	@PostMapping("/brand")
	@RequiresPermissions(value = "brand:add")
	@SystemControllerLog(description = "添加品牌")
	public String addUser(@ModelAttribute("brand") Brand brand) {
		this.brandService.saveOrUpdate(brand);
		return "redirect:brands";
	}

	@PutMapping("/brand")
	@RequiresPermissions(value = "brand:edit")
	@SystemControllerLog(description = "修改品牌")
	public String edit(@ModelAttribute("brand") Brand brand) {
		this.brandService.saveOrUpdate(brand);
		return "redirect:brands";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/brand{id}")
	@RequiresPermissions(value = "brand:edit")
	@SystemControllerLog(description = "编辑品牌")
	public String toeditPage(@PathVariable String id, Model model) {
		Brand brand = this.brandService.findOneById(id, Brand.class);
		model.addAttribute("brand", brand);
		return "admin/brand/add";

	}

	@DeleteMapping("/brand/{id}")
	@RequiresPermissions(value = "brand:delete")
	@SystemControllerLog(description = "删除品牌")
	public String delete(@PathVariable String id) {
		log.info("删除品牌" + id);
		this.brandService.delete(id);
		log.info("删除品牌" + id + "成功");
		return "redirect:/brands";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/brand/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		return this.brandService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/brand/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.brandService.todisable(id);
	}
	
	
	
	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/brand/download")
	@SystemControllerLog(description = "下载品牌信息导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "品牌管理模版.xlsx";
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
	@RequestMapping(value = "/brand/upload")
	@SystemControllerLog(description = "批量导入品牌信息")
	@RequiresPermissions(value = "brand:batch")
	public ModelAndView upload( HttpServletRequest request, HttpSession session,RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/brands");
		String error = this.brandService.upload(request, session);
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
	@RequestMapping(value = "/brand/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.brandService.findproInfo(request);
	}
	
	
	

}
