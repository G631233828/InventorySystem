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
import zhongchiedu.inventory.pojo.NewCustomer;
import zhongchiedu.inventory.service.Impl.NewCustomerServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 * newCustomer Controller  copy by Ch
 */
@Controller
@Slf4j
public class NewCustomerController {

	@Autowired
	private NewCustomerServiceImpl newCustomerService;


	@GetMapping("newCustomers")
	@RequiresPermissions(value = "newCustomer:list")
	@SystemControllerLog(description = "查询所有客户信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
			model.addAttribute("errorImport", errorImport);
		Pagination<NewCustomer> pagination = this.newCustomerService.findpagination(pageNo, pageSize,"");
		model.addAttribute("pageList", pagination);
		return "admin/newCustomer/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/newCustomer")
	@RequiresPermissions(value = "newCustomer:add")
	public String addPage(Model model) {
		return "admin/newCustomer/add";
	}

	@PostMapping("/newCustomer")
	@RequiresPermissions(value = "newCustomer:add")
	@SystemControllerLog(description = "添加客户")
	public String addnewCustomer(@ModelAttribute("newCustomer") NewCustomer newCustomer) {
		this.newCustomerService.saveOrUpdate(newCustomer);
		return "redirect:newCustomers";
	}

	@PutMapping("/newCustomer")
	@RequiresPermissions(value = "newCustomer:edit")
	@SystemControllerLog(description = "修改客户")
	public String edit(@ModelAttribute("newCustomer") NewCustomer newCustomer) {
		this.newCustomerService.saveOrUpdate(newCustomer);
		return "redirect:newCustomers";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/newCustomer{id}")
	@RequiresPermissions(value = "newCustomer:edit")
	@SystemControllerLog(description = "编辑客户")
	public String toeditPage(@PathVariable String id, Model model) {
		NewCustomer newCustomer = this.newCustomerService.findOneById(id, NewCustomer.class);
		model.addAttribute("newCustomer", newCustomer);
		return "admin/newCustomer/add";

	}

	@DeleteMapping("/newCustomer/{id}")
	@RequiresPermissions(value = "newCustomer:delete")
	@SystemControllerLog(description = "删除客户")
	public String delete(@PathVariable String id) {
		log.info("删除客户" + id);
		this.newCustomerService.delete(id);
		log.info("删除客户" + id + "成功");
		return "redirect:/newCustomers";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 *
	 */
	@RequestMapping(value = "/newCustomer/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		log.info("jinrufangfa");
		return this.newCustomerService.ajaxgetRepletes(name);
	}


	@RequestMapping(value = "/newCustomer/ajaxgetCustomer", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetCustomer(@RequestParam(value = "name", defaultValue = "") String name
	) {

		return this.newCustomerService.ajaxgetCustomer(name);
	}

	@RequestMapping(value = "/newCustomer/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.newCustomerService.todisable(id);
	}




	/**
	 * 模版下载
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/newCustomer/download")
	@SystemControllerLog(description = "下载客户信息导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "客户管理模版.xlsx";
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
	@RequestMapping(value = "/newCustomer/upload")
	@SystemControllerLog(description = "批量导入品牌信息")
	@RequiresPermissions(value = "newCustomer:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/newCustomers");
		String error = this.newCustomerService.upload(request, session);
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
	@RequestMapping(value = "/newCustomer/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.newCustomerService.findproInfo(request);
	}


//	@RequestMapping(value = "/newCustomer/a", method = RequestMethod.GET)
//	@ResponseBody
//	public BasicDataResult ajaxgetCustomer(HttpServletRequest request, HttpServletResponse response)
//	{
//
//		newCustomerService.createCuster();
//		newCustomerService.createPname();
//		return BasicDataResult.ok("200");
//	}


}
