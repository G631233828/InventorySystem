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
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechat")
public class AreaController {

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
	private AreaService areaService;

	/**
	 * 跳转库存界面
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "areas", method = RequestMethod.GET)
	@RequiresPermissions(value = "area:list")
	@SystemControllerLog(description = "查询所有归属")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search) {

		Pagination<Area> pagination = this.areaService.findpagination(pageNo, pageSize,search);
		model.addAttribute("pageList", pagination);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);

		return "area/area";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/area")
	@RequiresPermissions(value = "area:add")
	public String addPage(Model model) {
		return "area/edit";
	}
	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/area/{id}")
	@RequiresPermissions(value = "area:add")
	public String editPage(Model model,@PathVariable String id) {
		Area area = this.areaService.findOneById(id, Area.class);
		model.addAttribute("area", area);
		return "area/edit";
	}

	@PostMapping(value = "/area")
	@RequiresPermissions(value = "area:edit")
	public String addProjectStock(Model model,Area area) {
		
		this.areaService.saveOrUpdate(area);
		
		return "redirect:/wechat/areas";
	}
	
	@DeleteMapping("/area/{id}")
	@RequiresPermissions(value = "area:delete")
	@SystemControllerLog(description = "删除设备")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		this.areaService.delete(id);
		return "redirect:/wechat/areas";

	}

	
	
	
}
