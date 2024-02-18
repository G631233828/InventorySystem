package zhongchiedu.controller.inventory;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Pname;
import zhongchiedu.inventory.service.Impl.PnameServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 * pName Controller  copy by Ch
 */
@Controller
@Slf4j
public class PnameController {

	@Autowired
	private PnameServiceImpl pnameService;


	@GetMapping("pNames")
	@RequiresPermissions(value = "pName:list")
	@SystemControllerLog(description = "查询所有项目名称信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
			model.addAttribute("errorImport", errorImport);
		Pagination<Pname> pagination = this.pnameService.findpagination(pageNo, pageSize,"");
		model.addAttribute("pageList", pagination);
		return "admin/pName/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/pName")
	@RequiresPermissions(value = "pName:add")
	public String addPage(Model model) {
		return "admin/pName/add";
	}

	@PostMapping("/pName")
	@RequiresPermissions(value = "pName:add")
	@SystemControllerLog(description = "添加项目名称")
	public String addpName(@ModelAttribute("pName") Pname pName) {
		this.pnameService.saveOrUpdate(pName);
		return "redirect:pNames";
	}

	@PutMapping("/pName")
	@RequiresPermissions(value = "pName:edit")
	@SystemControllerLog(description = "修改项目名称")
	public String edit(@ModelAttribute("pName") Pname pName) {
		this.pnameService.saveOrUpdate(pName);
		return "redirect:pNames";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/pName{id}")
	@RequiresPermissions(value = "pName:edit")
	@SystemControllerLog(description = "编辑项目名称")
	public String toeditPage(@PathVariable String id, Model model) {
		Pname pName = this.pnameService.findOneById(id, Pname.class);
		model.addAttribute("pName", pName);
		return "admin/pName/add";

	}

	@DeleteMapping("/pName/{id}")
	@RequiresPermissions(value = "pName:delete")
	@SystemControllerLog(description = "删除项目名称")
	public String delete(@PathVariable String id) {
		log.info("删除项目名称" + id);
		this.pnameService.delete(id);
		log.info("删除项目名称" + id + "成功");
		return "redirect:/pNames";
	}

	/**
	 * 通过ajax获取是否存在重复项目名称的信息
	 *
	 */
	@RequestMapping(value = "/pName/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		log.info("jinrufangfa");
		return this.pnameService.ajaxgetRepletes(name);
	}


	@RequestMapping(value = "/pName/ajaxgetName", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetName(@RequestParam(value = "name", defaultValue = "") String name
	) {

		return this.pnameService.ajaxgetName(name);
	}

	@RequestMapping(value = "/pName/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.pnameService.todisable(id);
	}




	/**
	 * 模版下载
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/pName/download")
	@SystemControllerLog(description = "下载项目名称信息导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "项目名称模版.xlsx";
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
	@RequestMapping(value = "/pName/upload")
	@SystemControllerLog(description = "批量导入项目名称")
	@RequiresPermissions(value = "pName:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/pNames");
		String error = this.pnameService.upload(request, session);
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
	@RequestMapping(value = "/pName/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.pnameService.findproInfo(request);
	}






}
