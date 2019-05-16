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
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.service.Impl.SystemClassificationServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;


/**
 * 系统分类
 * @author fliay
 *
 */
@Controller
@Slf4j
public class SystemClassificationController {

	@Autowired
	private SystemClassificationServiceImpl systemClassificationService;


	@GetMapping("systemClassifications")
	@RequiresPermissions(value = "systemClassification:list")
	@SystemControllerLog(description = "查询所有系统分类信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
			model.addAttribute("errorImport", errorImport);
		Pagination<SystemClassification> pagination = this.systemClassificationService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);
		return "admin/systemClassification/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/systemClassification")
	@RequiresPermissions(value = "systemClassification:add")
	public String addPage(Model model) {
		return "admin/systemClassification/add";
	}

	@PostMapping("/systemClassification")
	@RequiresPermissions(value = "systemClassification:add")
	@SystemControllerLog(description = "添加类目")
	public String addUser(@ModelAttribute("systemClassification") SystemClassification systemClassification) {
		this.systemClassificationService.saveOrUpdate(systemClassification);
		return "redirect:systemClassifications";
	}

	@PutMapping("/systemClassification")
	@RequiresPermissions(value = "systemClassification:edit")
	@SystemControllerLog(description = "修改类目")
	public String edit(@ModelAttribute("systemClassification") SystemClassification systemClassification) {
		this.systemClassificationService.saveOrUpdate(systemClassification);
		return "redirect:systemClassifications";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/systemClassification{id}")
	@RequiresPermissions(value = "systemClassification:edit")
	@SystemControllerLog(description = "编辑类目")
	public String toeditPage(@PathVariable String id, Model model) {
		SystemClassification systemClassification = this.systemClassificationService.findOneById(id, SystemClassification.class);
		model.addAttribute("systemClassification", systemClassification);
		
		
		//获得选中的类目集合放到selectCategorys中
//		List<CaseType> listc = casePresentation.getCaseTypes();
//		if (Common.isNotEmpty(listc)) {
//			List<String> lists = new ArrayList<>();
//			for (int i = 0; i < listc.size(); i++) {
//				lists.add(listc.get(i).getId());
//			}
//			caseTypes = lists.toArray();
//			model.addAttribute("caseTypes", caseTypes);
//		}
		
		
		
		
		
		return "admin/systemClassification/add";

	}

	@DeleteMapping("/systemClassification/{id}")
	@RequiresPermissions(value = "systemClassification:delete")
	@SystemControllerLog(description = "删除系统分类")
	public String delete(@PathVariable String id) {
		log.info("删除系统分类" + id);
		this.systemClassificationService.delete(id);
		log.info("删除系统分类" + id + "成功");
		return "redirect:/systemClassifications";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/systemClassification/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		return this.systemClassificationService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/systemClassification/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.systemClassificationService.todisable(id);
	}
	
	
	
	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/systemClassification/download")
	@SystemControllerLog(description = "下载类目信息导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "系统分类模版.xlsx";
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
	@RequestMapping(value = "/systemClassification/upload")
	@SystemControllerLog(description = "批量导入类目信息")
	@RequiresPermissions(value = "systemClassification:batch")
	public ModelAndView upload( HttpServletRequest request, HttpSession session,RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/systemClassifications");
		String error = this.systemClassificationService.upload(request, session);
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
	@RequestMapping(value = "/systemClassification/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.systemClassificationService.findproInfo(request);
	}
	
	
	

}
