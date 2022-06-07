package zhongchiedu.controller.inventory;

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
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class AreaController {

	@Autowired
	private AreaServiceImpl areaService;


	@GetMapping("areas")
	@RequiresPermissions(value = "area:list")
	@SystemControllerLog(description = "查询所有区域信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
			model.addAttribute("errorImport", errorImport);
		Pagination<Area> pagination = this.areaService.findpagination(pageNo, pageSize,"");
		model.addAttribute("pageList", pagination);
		return "admin/area/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/area")
	@RequiresPermissions(value = "area:add")
	public String addPage(Model model) {
		return "admin/area/add";
	}

	@PostMapping("/area")
	@RequiresPermissions(value = "area:add")
	@SystemControllerLog(description = "添加区域")
	public String addUser(@ModelAttribute("area") Area area) {
		this.areaService.saveOrUpdate(area);
		return "redirect:areas";
	}

	@PutMapping("/area")
	@RequiresPermissions(value = "area:edit")
	@SystemControllerLog(description = "修改区域")
	public String edit(@ModelAttribute("area") Area area) {
		this.areaService.saveOrUpdate(area);
		return "redirect:areas";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/area{id}")
	@RequiresPermissions(value = "area:edit")
	@SystemControllerLog(description = "编辑区域")
	public String toeditPage(@PathVariable String id, Model model) {
		Area area = this.areaService.findOneById(id, Area.class);
		model.addAttribute("area", area);
		return "admin/area/add";

	}

	@DeleteMapping("/area/{id}")
	@RequiresPermissions(value = "area:delete")
	@SystemControllerLog(description = "删除区域")
	public String delete(@PathVariable String id) {
		log.info("删除区域" + id);
		this.areaService.delete(id);
		log.info("删除区域" + id + "成功");
		return "redirect:/areas";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/area/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		return this.areaService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/area/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.areaService.todisable(id);
	}
	
	
	
	
	

	

}
