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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.GoodsStorageService;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechat")
public class GoodsStorageController {

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
	private GoodsStorageService goodsStorageService;

	@Autowired
	private AreaService areaService;
	
	
	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "goodsStorages", method = RequestMethod.GET)
	@RequiresPermissions(value = "goodsStorage:list")
	@SystemControllerLog(description = "查询所有货架")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search) {

		Pagination<GoodsStorage> pagination = this.goodsStorageService.findpagination(pageNo, pageSize, "",search);
		model.addAttribute("pageList", pagination);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);

		List<Area> arealist = this.areaService.findAllArea(false);
		model.addAttribute("arealist", arealist);
		
		
		return "goodsStorage/goodsStorage";
	}



	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/goodsStorage")
	@RequiresPermissions(value = "goodsStorage:add")
	public String addPage(Model model) {
		List<Area> arealist = this.areaService.findAllArea(false);
		model.addAttribute("areas", arealist);
		return "goodsStorage/edit";
	}
	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/goodsStorage/{id}")
	@RequiresPermissions(value = "goodsStorage:add")
	public String editPage(Model model,@PathVariable String id) {
		GoodsStorage goodsStorage = this.goodsStorageService.findOneById(id, GoodsStorage.class);
		model.addAttribute("goodsStorage", goodsStorage);
		List<Area> arealist = this.areaService.findAllArea(false);
		model.addAttribute("areas", arealist);
		return "goodsStorage/edit";
	}

	@PostMapping(value = "/goodsStorage")
	@RequiresPermissions(value = "goodsStorage:edit")
	public String addProjectStock(Model model,GoodsStorage goodsStorage) {
		
		this.goodsStorageService.saveOrUpdate(goodsStorage);
		
		return "redirect:/wechat/goodsStorages";
	}
	
	@DeleteMapping("/goodsStorage/{id}")
	@RequiresPermissions(value = "goodsStorage:delete")
	@SystemControllerLog(description = "删除设备")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		this.goodsStorageService.delete(id);
		return "redirect:/wechat/goodsStorages";

	}

	
	
	
}
