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
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.GoodsStorageService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechat")
public class StockController {

	@Autowired
	private WxMpService wxMpService;

	@Autowired
	private UserService userService;

	@Autowired
	private WxMpProperties wxMpProperties;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	private @Autowired StockStatisticsService stockStatisticsService;
	
	private @Autowired GoodsStorageService goodsStorageService;

	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;

	@Autowired
	private StockService stockService;

	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "stocks", method = RequestMethod.GET)
	@RequiresPermissions(value = "stock:list")
	@SystemControllerLog(description = "查询所有库存管理")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search) {

		Pagination<Stock> pagination = this.stockService.findpagination(pageNo, pageSize, search, "");
		model.addAttribute("pageList", pagination);

		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);

		return "stock/stock";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/stock")
	@RequiresPermissions(value = "stock:add")
	public String addPage(Model model) {
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);

		return "stock/edit";
	}
	
	
	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/stock/{id}", method = RequestMethod.GET)
	@RequiresPermissions(value = "stock:edit")
	@SystemControllerLog(description = "修改库存信息")
	public String editStock(Model model, @PathVariable String id) {
		
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		Stock stock = this.stockService.findOneById(id, Stock.class);
		model.addAttribute("stock", stock);
		if (Common.isNotEmpty(stock.getArea())) {
			// 所有货架
			List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false, stock.getArea().getId());
			model.addAttribute("goodsStorages", list);
		}

		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		
		
		return "stock/edit";
	}
	
	
	
	
	/**
	 * 根据选择的目录获取菜单
	 * 
	 * @param parentId
	 * @return
	 */
	@RequestMapping(value = "/getStorages", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getparentmenu(@RequestParam(value = "areaId", defaultValue = "") String areaId) {

		List<GoodsStorage> list = this.goodsStorageService.findStorages(areaId);

		return list != null ? BasicDataResult.build(200, "success", list) : BasicDataResult.build(400, "error", null);
	}

	/**
	 * 商品入库界面
	 * 
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/stock_in/{id}")
	@RequiresPermissions(value = "stock:add")
	public String stock_in(Model model, @PathVariable String id) {

		Stock stock = this.stockService.findOneById(id, Stock.class);
		model.addAttribute("stock", stock);

		return "stock/in";
	}
	/**
	 * 商品入库界面
	 * 
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/stock_out/{id}")
	@RequiresPermissions(value = "stock:add")
	public String stock_out(Model model, @PathVariable String id) {
		
		Stock stock = this.stockService.findOneById(id, Stock.class);
		model.addAttribute("stock", stock);
		
		return "stock/out";
	}

	@PostMapping(value = "/stock")
	@RequiresPermissions(value = "stock:edit")
	public String addStock(Model model,Stock stock) {
		
		this.stockService.saveOrUpdate(stock);
		
		return "redirect:/wechat/stocks";
	}
	
	/**
	 * 商品入库
	 * 
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/stock/in")
	@ResponseBody
	@RequiresPermissions(value = "stock:add")
	public BasicDataResult in(@ModelAttribute("stockStatistics") StockStatistics stockStatistics, HttpSession session) {
		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
	}
	/**
	 * 商品出库
	 * 
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/stock/out")
	@ResponseBody
	@RequiresPermissions(value = "stock:add")
	public BasicDataResult out(@ModelAttribute("stockStatistics") StockStatistics stockStatistics, HttpSession session) {
		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
	}

	@DeleteMapping("/stock/{id}")
	@RequiresPermissions(value = "stock:delete")
	@SystemControllerLog(description = "删除设备")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		this.stockService.delete(id);
		return "redirect:/wechat/stocks";

	}

	
	
	
}
