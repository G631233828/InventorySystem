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
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.service.Impl.CompanyServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class CompanysController {

	@Autowired
	private CompanyServiceImpl companyService;

	@GetMapping("companys")
	@RequiresPermissions(value = "company:list")
	@SystemControllerLog(description = "查询所有企业")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session) {
		Pagination<Companys> pagination = this.companyService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		return "admin/companys/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/company")
	@RequiresPermissions(value = "company:add")
	public String addPage() {
		return "admin/companys/add";
	}

	@PostMapping("/company")
	@RequiresPermissions(value = "company:add")
	@SystemControllerLog(description = "添加企业")
	public String addUser(@ModelAttribute("companys") Companys companys) {
		this.companyService.saveOrUpdate(companys);
		return "redirect:companys";
	}
	
	
	@PutMapping("/company")
	@RequiresPermissions(value = "company:edit")
	@SystemControllerLog(description = "修改企业")
	public String edit(@ModelAttribute("companys") Companys companys) {
		this.companyService.saveOrUpdate(companys);
		return "redirect:companys";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/company{id}")
	@RequiresPermissions(value = "company:edit")
	@SystemControllerLog(description = "编辑企业")
	public String toeditPage(@PathVariable String id, Model model) {
		Companys companys = this.companyService.findOneById(id, Companys.class);
		model.addAttribute("company", companys);
		return "admin/companys/add";

	}

	@DeleteMapping("/company/{id}")
	@RequiresPermissions(value = "company:delete")
	@SystemControllerLog(description = "删除企业")
	public String delete(@PathVariable String id) {
		log.info("删除企业" + id);
		this.companyService.delete(id);
		log.info("删除企业" + id + "成功");
		return "redirect:/companys";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/company/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name) {
		return this.companyService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/company/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.companyService.todisable(id);
	}

}
