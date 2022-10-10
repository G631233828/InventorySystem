package zhongchiedu.controller.inventory;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.Workbook;
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
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.RepairManagement;
import zhongchiedu.inventory.service.RepairManagementService;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;



/**
 * 设备流转
 * @author gjb
 *
 */

@Controller
@Slf4j
public class RepairManagementController {
	
	
	private @Autowired ColumnServiceImpl columnService;
	
	private @Autowired RepairManagementService repairManagementService;
	
	
	
	@GetMapping("repairManagements")
	@RequiresPermissions(value = "repairManagement:list")
	@SystemControllerLog(description = "查询返修设备信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end,
			@RequestParam(value = "search", defaultValue = "") String search
			) {


		model.addAttribute("errorImport", errorImport);
		Pagination<RepairManagement> pagination = this.repairManagementService.findpagination(pageNo, pageSize,start,end, search);
		model.addAttribute("pageList", pagination);

		List<String> listColums = this.columnService.findColumns("repairManagement");
		model.addAttribute("listColums", listColums);

		session.setAttribute("pageNo", pageNo);
		session.setAttribute("pageSize", pageSize);
		session.setAttribute("search", search);
		session.setAttribute("start", start);
		session.setAttribute("end", end);
		model.addAttribute("start", start);
		model.addAttribute("end", end);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);

		return "admin/repairManagement/list";
	}
	
	
	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/repairManagement")
	@RequiresPermissions(value = "repairManagement:add")
	public String addPage(Model model) {

		return "admin/repairManagement/add";
	}
	
	
	@PostMapping("/repairManagement")
	@RequiresPermissions(value = "repairManagement:add")
	@SystemControllerLog(description = "添加返修设备")
	public String addUser(@ModelAttribute("repairManagement") RepairManagement repairManagement, HttpSession session)
			throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		repairManagement.setOperator(user.getUserName());
		this.repairManagementService.saveOrUpdate(repairManagement);
	
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String start = (String) session.getAttribute("start");
		String end = (String) session.getAttribute("end");

		return "redirect:/repairManagements?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8")+"&start="+start+"&end="+end ;

	}

	@PutMapping("/repairManagement")
	@RequiresPermissions(value = "repairManagement:edit")
	@SystemControllerLog(description = "修改返修设备")
	public String edit(@ModelAttribute("repairManagement") RepairManagement repairManagement, HttpSession session) throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		repairManagement.setOperator(user.getUserName());
		this.repairManagementService.saveOrUpdate(repairManagement);
	
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String start = (String) session.getAttribute("start");
		String end = (String) session.getAttribute("end");

			return "redirect:/repairManagements?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8")+"&start="+start+"&end="+end ;

	}
	
	
	
	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/repairManagement{id}")
	@RequiresPermissions(value = "repairManagement:edit")
	@SystemControllerLog(description = "编辑返修设备")
	public String toeditPage(@PathVariable String id, Model model) {
		
		RepairManagement repairManagement = this.repairManagementService.findOneById(id, RepairManagement.class);
		model.addAttribute("repairManagement", repairManagement);
		return "admin/repairManagement/add";

	}
	
	
	@RequestMapping("/repairManagement/clearSearch")
//	@RequiresPermissions(value = "projectStock:delete")
	public String clearSearch(HttpSession session) {
		session.removeAttribute("pageNo");
		session.removeAttribute("pageSize");
		session.removeAttribute("search");
		session.removeAttribute("start");
		session.removeAttribute("end");
		return "redirect:/repairManagements";

	}
	
	
	
	
	
	
	@DeleteMapping("/repairManagement/{id}")
	@RequiresPermissions(value = "repairManagement:delete")
	@SystemControllerLog(description = "删除返修设备")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		log.info("删除设备" + id);
		this.repairManagementService.delete(id);
		log.info("删除设备" + id + "成功");
		String start = (String) session.getAttribute("start");
		String end = (String) session.getAttribute("end");
		return "redirect:/repairManagements?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8")+"&start="+start+"&end="+end ;

	}

	
	
	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/repairManagement/download")
	@SystemControllerLog(description = "下载库存中转导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "返修设备导入模板.xlsx";
		                    
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
	@RequestMapping(value = "/repairManagement/upload")
	@SystemControllerLog(description = "批量导入返修设备")
	@RequiresPermissions(value = "repairManagement:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/repairManagements");
		String error = this.repairManagementService.upload(request, session);
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
	@RequestMapping(value = "/repairManagement/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.repairManagementService.findproInfo(request);
	}
	
	
	@RequestMapping(value = "/repairManagement/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag) {
		return this.columnService.editColumns("repairManagement", column, flag);
	}
	
	
	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/repairManagement/export", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportRepairManagement (@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end, HttpServletResponse response,
			HttpServletRequest request) throws Exception{
	
		String exportName = Common.fromDateYMD() + "返修设备统计";

		response.setContentType("application/vnd.ms-excel");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		Workbook newExport = this.repairManagementService.newExport(request, search, start, end);

		OutputStream out = response.getOutputStream();
		newExport.write(out);
		out.flush();
		out.close();
		
		
		
	}
	
	
	
	
	
	
	

}
