package zhongchiedu.controller.inventory;

import java.util.List;

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

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Companys;
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
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session) {
		Pagination<Category> pagination = this.categoryStorageService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		return "admin/Category/list";
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
	@RequiresPermissions(value = "company:delete")
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

}
