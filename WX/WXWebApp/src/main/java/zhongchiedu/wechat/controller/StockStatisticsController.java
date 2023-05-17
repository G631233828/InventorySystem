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
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechat")
public class StockStatisticsController {

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
	@RequestMapping(value = "stockStatisticss", method = RequestMethod.GET)
	@RequiresPermissions(value = "stockStatistics:list")
	@SystemControllerLog(description = "查询所有库存统计")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search) {
		
		Pagination<StockStatistics> pagination = this.stockStatisticsService.findpagination(pageNo, pageSize, search,
				"", "", "", "","","","","","");
		model.addAttribute("pageList", pagination);
		model.addAttribute("search", search);

		return "stockStatistics/stockStatistics";
	}

//	/**
//	 * 跳转到添加页面
//	 */
//	@GetMapping(value = "/stock")
//	@RequiresPermissions(value = "stock:add")
//	public String addPage(Model model) {
//		// 区域
//		List<Area> areas = this.areaService.findAllArea(false);
//		model.addAttribute("areas", areas);
//		return "stock/edit";
//	}
//
//	/**
//	 * 商品入库界面
//	 * 
//	 * @param model
//	 * @return
//	 */
//	@GetMapping(value = "/stock_in/{id}")
//	@RequiresPermissions(value = "stock:add")
//	public String stock_in(Model model, @PathVariable String id) {
//
//		Stock stock = this.stockService.findOneById(id, Stock.class);
//		model.addAttribute("stock", stock);
//
//		return "stock/in";
//	}
//	/**
//	 * 商品入库界面
//	 * 
//	 * @param model
//	 * @return
//	 */
//	@GetMapping(value = "/stock_out/{id}")
//	@RequiresPermissions(value = "stock:add")
//	public String stock_out(Model model, @PathVariable String id) {
//		
//		Stock stock = this.stockService.findOneById(id, Stock.class);
//		model.addAttribute("stock", stock);
//		
//		return "stock/out";
//	}
//
//	/**
//	 * 商品入库
//	 * 
//	 * @param model
//	 * @return
//	 */
//	@PostMapping(value = "/stock/in")
//	@ResponseBody
//	@RequiresPermissions(value = "stock:add")
//	public BasicDataResult in(@ModelAttribute("stockStatistics") StockStatistics stockStatistics, HttpSession session) {
//		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
//		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
//	}
//	/**
//	 * 商品出库
//	 * 
//	 * @param model
//	 * @return
//	 */
//	@PostMapping(value = "/stock/out")
//	@ResponseBody
//	@RequiresPermissions(value = "stock:add")
//	public BasicDataResult out(@ModelAttribute("stockStatistics") StockStatistics stockStatistics, HttpSession session) {
//		User user = (User) session.getAttribute(Contents.WECHAT_WEB_SESSION);
//		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
//	}
//
//	@DeleteMapping("/stock/{id}")
//	@RequiresPermissions(value = "stock:delete")
//	@SystemControllerLog(description = "删除设备")
//	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
//		this.stockService.delete(id);
//		return "redirect:/wechat/stocks";
//
//	}
//
//	
	
	
}
