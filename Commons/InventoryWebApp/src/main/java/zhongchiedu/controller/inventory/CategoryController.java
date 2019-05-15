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
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.service.Impl.CategoryServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class CategoryController {

	@Autowired
	private CategoryServiceImpl categoryStorageService;


	@GetMapping("categorys")
	@RequiresPermissions(value = "category:list")
	@SystemControllerLog(description = "查询所有货架信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
		
		
			model.addAttribute("errorImport", errorImport);
		Pagination<Category> pagination = this.categoryStorageService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		return "admin/category/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/category")
	@RequiresPermissions(value = "category:add")
	public String addPage(Model model) {
		return "admin/category/add";
	}

	@PostMapping("/category")
	@RequiresPermissions(value = "category:add")
	@SystemControllerLog(description = "添加类目")
	public String addUser(@ModelAttribute("category") Category Category) {
		this.categoryStorageService.saveOrUpdate(Category);
		return "redirect:categorys";
	}

	@PutMapping("/category")
	@RequiresPermissions(value = "category:edit")
	@SystemControllerLog(description = "修改类目")
	public String edit(@ModelAttribute("Category") Category category) {
		this.categoryStorageService.saveOrUpdate(category);
		return "redirect:categorys";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/category{id}")
	@RequiresPermissions(value = "category:edit")
	@SystemControllerLog(description = "编辑类目")
	public String toeditPage(@PathVariable String id, Model model) {
		Category category = this.categoryStorageService.findOneById(id, Category.class);
		model.addAttribute("category", category);
		return "admin/category/add";

	}

	@DeleteMapping("/category/{id}")
	@RequiresPermissions(value = "category:delete")
	@SystemControllerLog(description = "删除货架")
	public String delete(@PathVariable String id) {
		log.info("删除货架" + id);
		this.categoryStorageService.delete(id);
		log.info("删除货架" + id + "成功");
		return "redirect:/categorys";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/category/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		return this.categoryStorageService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/category/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.categoryStorageService.todisable(id);
	}
	
	
	
	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/category/download")
	@SystemControllerLog(description = "下载类目信息导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "类目管理模版.xlsx";
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
	@RequestMapping(value = "/category/upload")
	@SystemControllerLog(description = "批量导入类目信息")
	@RequiresPermissions(value = "category:batch")
	public ModelAndView upload( HttpServletRequest request, HttpSession session,RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/categorys");
		String error = this.categoryStorageService.upload(request, session);
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
	@RequestMapping(value = "/category/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.categoryStorageService.findproInfo(request);
	}
	
	
	

}
