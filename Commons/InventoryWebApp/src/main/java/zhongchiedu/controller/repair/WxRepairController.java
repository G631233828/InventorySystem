package zhongchiedu.controller.repair;

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
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.service.Impl.WxRepairServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
@RequestMapping("/repair")
public class WxRepairController {

	@Autowired
	private WxRepairServiceImpl wxRepairService;


	@GetMapping("/wxRepairs")
	@RequiresPermissions(value = "wxRepair:list")
	@SystemControllerLog(description = "查询所有报修信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session) {
		Pagination<WxRepair> pagination = this.wxRepairService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		
		return "school/wxrepair/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/wxRepair")
	@RequiresPermissions(value = "wxRepair:add")
	public String addPage() {
		return "school/wxrepair/add";
	}

//	@PostMapping("/wxRepair")
//	@RequiresPermissions(value = "wxRepaire:add")
//	@SystemControllerLog(description = "添加货架")
//	public String addUser(@ModelAttribute("wxRepair") WxRepair wxRepair) {
//		this.wxRepairService.saveOrUpdate(wxRepair);
//		return "redirect:wxRepairs";
//	}
//
//	@PutMapping("/wxRepair")
//	@RequiresPermissions(value = "wxRepair:edit")
//	@SystemControllerLog(description = "修改报修信息")
//	public String edit(@ModelAttribute("wxRepair") WxRepair wxRepair) {
//		this.wxRepairService.saveOrUpdate(wxRepair);
//		return "redirect:wxRepairs";
//	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/wxRepair{id}")
	@RequiresPermissions(value = "wxRepair:edit")
	@SystemControllerLog(description = "编辑报修信息")
	public String toeditPage(@PathVariable String id, Model model) {
		WxRepair wxRepair = this.wxRepairService.findOneById(id, WxRepair.class);
		model.addAttribute("wxRepair", wxRepair);
		return "school/wxRepair/add";

	}
	
	
	
	
	
	
	
	
	
	

	@DeleteMapping("/wxRepair/{id}")
	@RequiresPermissions(value = "wxRepair:delete")
	@SystemControllerLog(description = "删除报修信息")
	public String delete(@PathVariable String id) {
		log.info("删除报修" + id);
		this.wxRepairService.delete(id);
		log.info("删除报修" + id + "成功");
		return "redirect:wxRepairs";
	}
	
	


}
