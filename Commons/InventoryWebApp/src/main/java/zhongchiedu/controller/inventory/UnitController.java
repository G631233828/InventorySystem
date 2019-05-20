package zhongchiedu.controller.inventory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class UnitController {

	@Autowired
	private UnitServiceImpl unitService;


	@GetMapping("units")
	@RequiresPermissions(value = "unit:list")
	@SystemControllerLog(description = "查询所有单位信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
			model.addAttribute("errorImport", errorImport);
		Pagination<Unit> pagination = this.unitService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		return "admin/unit/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/unit")
	@RequiresPermissions(value = "unit:add")
	public String addPage(Model model) {
		return "admin/unit/add";
	}

	@PostMapping("/unit")
	@RequiresPermissions(value = "unit:add")
	@SystemControllerLog(description = "添加单位")
	public String addUser(@ModelAttribute("unit") Unit unit) {
		this.unitService.saveOrUpdate(unit);
		return "redirect:units";
	}

	@PutMapping("/unit")
	@RequiresPermissions(value = "unit:edit")
	@SystemControllerLog(description = "修改单位")
	public String edit(@ModelAttribute("unit") Unit unit) {
		this.unitService.saveOrUpdate(unit);
		return "redirect:units";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/unit{id}")
	@RequiresPermissions(value = "unit:edit")
	@SystemControllerLog(description = "编辑单位")
	public String toeditPage(@PathVariable String id, Model model) {
		Unit unit = this.unitService.findOneById(id, Unit.class);
		model.addAttribute("unit", unit);
		return "admin/unit/add";

	}

	@DeleteMapping("/unit/{id}")
	@RequiresPermissions(value = "unit:delete")
	@SystemControllerLog(description = "删除单位")
	public String delete(@PathVariable String id) {
		log.info("删除单位" + id);
		this.unitService.delete(id);
		log.info("删除单位" + id + "成功");
		return "redirect:/units";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/unit/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		return this.unitService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/unit/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.unitService.todisable(id);
	}
	
	
	
	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/unit/download")
	@SystemControllerLog(description = "下载类目信息导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "单位管理模版.xlsx";
		String contentType = "application/octet-stream";
		String UPLOAD = "Templates/";
		FileOperateUtil.download(request, response, storeName, contentType, UPLOAD);
		return null;
	}
	
	/***
	 * 文件上传
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/unit/upload")
	@SystemControllerLog(description = "批量导入单位信息")
	@RequiresPermissions(value = "unit:batch")
	public ModelAndView upload( HttpServletRequest request, HttpSession session,RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/units");
		String error = this.unitService.upload(request, session);
		attr.addFlashAttribute("errorImport", error);
		return modelAndView;

	}

	/**
	 * process 获取进度
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/unit/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.unitService.findproInfo(request);
	}
	
	
	

}
