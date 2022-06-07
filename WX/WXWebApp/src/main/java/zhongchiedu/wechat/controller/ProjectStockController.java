package zhongchiedu.wechat.controller;

import java.io.UnsupportedEncodingException;
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
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.ProjectStockStatistics;
import zhongchiedu.inventory.service.ProjectStockService;
import zhongchiedu.inventory.service.ProjectStockStatisticsService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechat")
public class ProjectStockController {

	@Autowired
	private WxMpService wxMpService;

	@Autowired
	private UserService userService;

	@Autowired
	private WxMpProperties wxMpProperties;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	private @Autowired ProjectStockStatisticsService projectStockStatisticsService;

	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;

	@Autowired
	private ProjectStockService projectStockService;

	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "projectStocks", method = RequestMethod.GET)
	@RequiresPermissions(value = "projectStock:list")
	@SystemControllerLog(description = "查询所有库存管理")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search) {

		Pagination<ProjectStock> pagination = this.projectStockService.findpagination(pageNo, pageSize, search, "","");
		model.addAttribute("pageList", pagination);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);
		return "projectStock/projectStock";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/projectStock")
	@RequiresPermissions(value = "projectStock:add")
	public String addPage(Model model) {
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		return "projectStock/edit";
	}
	
	/**
	 * 修改项目库存
	 */
	@GetMapping(value = "/projectStock/{id}")
	@RequiresPermissions(value = "projectStock:edit")
	public String editPage(Model model,@PathVariable String id) {
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		ProjectStock projectStock = this.projectStockService.findOneById(id, ProjectStock.class);
		model.addAttribute("projectStock", projectStock);
		
		return "projectStock/edit";
	}
	
	@PostMapping(value = "/projectStock")
	@RequiresPermissions(value = "projectStock:edit")
	public String addProjectStock(Model model,ProjectStock projectStock) {
		
		this.projectStockService.saveOrUpdate(projectStock,projectStock.getArea().getId());
		
		return "redirect:/wechat/projectStocks";
	}
	
	

	/**
	 * 商品入库界面
	 * 
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/projectStock_in/{id}")
	@RequiresPermissions(value = "projectStock:add")
	public String projectStock_in(Model model, @PathVariable String id) {

		ProjectStock projectStock = this.projectStockService.findOneById(id, ProjectStock.class);
		model.addAttribute("projectStock", projectStock);

		return "projectStock/in";
	}
	/**
	 * 商品入库界面
	 * 
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/projectStock_out/{id}")
	@RequiresPermissions(value = "projectStock:add")
	public String projectStock_out(Model model, @PathVariable String id) {
		
		ProjectStock projectStock = this.projectStockService.findOneById(id, ProjectStock.class);
		model.addAttribute("projectStock", projectStock);
		
		return "projectStock/out";
	}

	/**
	 * 商品入库
	 * 
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/projectStock/in")
	@ResponseBody
	@RequiresPermissions(value = "projectStock:add")
	public BasicDataResult in(@ModelAttribute("projectStockStatistics") ProjectStockStatistics projectStockStatistics, HttpSession session) {
		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		return this.projectStockStatisticsService.inOrOutstockStatistics(projectStockStatistics, user);
	}
	/**
	 * 商品出库
	 * 
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/projectStock/out")
	@ResponseBody
	@RequiresPermissions(value = "projectStock:add")
	public BasicDataResult out(@ModelAttribute("projectStockStatistics") ProjectStockStatistics projectStockStatistics, HttpSession session) {
		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		return this.projectStockStatisticsService.inOrOutstockStatistics(projectStockStatistics, user);
	}

	@DeleteMapping("/projectStock/{id}")
	@RequiresPermissions(value = "projectStock:delete")
	@SystemControllerLog(description = "删除设备")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		this.projectStockService.delete(id);
		return "redirect:/wechat/projectStocks";

	}

	
	
	
}
