package zhongchiedu.wechat.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.service.CategoryService;
import zhongchiedu.inventory.service.CategoryService;
import zhongchiedu.inventory.service.Impl.CategoryServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechat")
public class CategoryController {

	@Autowired
	private WxMpService wxMpService;

	@Autowired
	private UserService userService;

	@Autowired
	private WxMpProperties wxMpProperties;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;


	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;

	@Autowired
	private CategoryService categoryService;

	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "categorys", method = RequestMethod.GET)
	@RequiresPermissions(value = "category:list")
	@SystemControllerLog(description = "查询所有类目")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search) {

		Pagination<Category> pagination = this.categoryService.findpagination(pageNo, pageSize,search);
		model.addAttribute("pageList", pagination);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);

		return "category/category";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/category")
	@RequiresPermissions(value = "category:add")
	public String addPage(Model model) {
		return "category/edit";
	}
	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/category/{id}")
	@RequiresPermissions(value = "category:add")
	public String editPage(Model model,@PathVariable String id) {
		Category category = this.categoryService.findOneById(id, Category.class);
		model.addAttribute("category", category);
		return "category/edit";
	}

	@PostMapping(value = "/category")
	@RequiresPermissions(value = "category:edit")
	public String addProjectStock(Model model,Category category) {
		this.categoryService.saveOrUpdate(category);
		return "redirect:/wechat/categorys";
	}
	
	@DeleteMapping("/category/{id}")
	@RequiresPermissions(value = "category:delete")
	@SystemControllerLog(description = "删除类目")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		this.categoryService.delete(id);
		return "redirect:/wechat/categorys";

	}

	
	
	
}
