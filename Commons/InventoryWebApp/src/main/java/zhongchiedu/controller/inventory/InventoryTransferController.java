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
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.InventoryTransfer;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.InventoryTransferService;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;



/**
 * 设备流转
 * @author gjb
 *
 */

@Controller
@Slf4j
public class InventoryTransferController {
	
	
	private @Autowired ColumnServiceImpl columnService;
	
	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;	
	
	private @Autowired InventoryTransferService inventoryTransferService;
	
	
	
	@GetMapping("inventoryTransfers")
	@RequiresPermissions(value = "inventoryTransfer:list")
	@SystemControllerLog(description = "查询所有中转库存")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end,
			@RequestParam(value = "search", defaultValue = "") String search
			) {


		model.addAttribute("errorImport", errorImport);
		Pagination<InventoryTransfer> pagination = this.inventoryTransferService.findpagination(pageNo, pageSize,start,end, search);
		model.addAttribute("pageList", pagination);
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		List<String> listColums = this.columnService.findColumns("inventoryTransfer",user.getId());
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

		return "admin/inventoryTransfer/list";
	}
	
	
	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/inventoryTransfer")
	@RequiresPermissions(value = "inventoryTransfer:add")
	public String addPage(Model model) {

		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);

		return "admin/inventoryTransfer/add";
	}
	
	
	@PostMapping("/inventoryTransfer")
	@RequiresPermissions(value = "inventoryTransfer:add")
	@SystemControllerLog(description = "添加中转库存")
	public String addUser(@ModelAttribute("inventoryTransfer") InventoryTransfer inventoryTransfer, HttpSession session)
			throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		inventoryTransfer.setOperator(user.getUserName());
		this.inventoryTransferService.saveOrUpdate(inventoryTransfer);
	
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String start = (String) session.getAttribute("start");
		String end = (String) session.getAttribute("end");

		return "redirect:/inventoryTransfers?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8")+"&start="+start+"&end="+end ;

	}

	@PutMapping("/inventoryTransfer")
	@RequiresPermissions(value = "inventoryTransfer:edit")
	@SystemControllerLog(description = "修改中转库存")
	public String edit(@ModelAttribute("inventoryTransfer") InventoryTransfer inventoryTransfer, HttpSession session) throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		inventoryTransfer.setOperator(user.getUserName());
		this.inventoryTransferService.saveOrUpdate(inventoryTransfer);
	
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String start = (String) session.getAttribute("start");
		String end = (String) session.getAttribute("end");

			return "redirect:/inventoryTransfers?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8")+"&start="+start+"&end="+end ;

	}
	
	
	
	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/inventoryTransfer{id}")
	@RequiresPermissions(value = "inventoryTransfer:edit")
	@SystemControllerLog(description = "编辑中转库存")
	public String toeditPage(@PathVariable String id, Model model) {
		
		InventoryTransfer inventoryTransfer = this.inventoryTransferService.findOneById(id, InventoryTransfer.class);
		model.addAttribute("inventoryTransfer", inventoryTransfer);

		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
		return "admin/inventoryTransfer/add";

	}
	
	
	@RequestMapping("/inventoryTransfer/clearSearch")
//	@RequiresPermissions(value = "projectStock:delete")
	public String clearSearch(HttpSession session) {
		session.removeAttribute("pageNo");
		session.removeAttribute("pageSize");
		session.removeAttribute("search");
		session.removeAttribute("start");
		session.removeAttribute("end");
		return "redirect:/inventoryTransfers";

	}
	
	
	
	
	
	
	@DeleteMapping("/inventoryTransfer/{id}")
	@RequiresPermissions(value = "inventoryTransfer:delete")
	@SystemControllerLog(description = "删除库存中转")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		log.info("删除设备" + id);
		this.inventoryTransferService.delete(id);
		log.info("删除设备" + id + "成功");
		String start = (String) session.getAttribute("start");
		String end = (String) session.getAttribute("end");
		return "redirect:/inventoryTransfers?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
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
	@RequestMapping(value = "/inventoryTransfer/download")
	@SystemControllerLog(description = "下载库存中转导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "库存流转导入模板.xlsx";
		                    
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
	@RequestMapping(value = "/inventoryTransfer/upload")
	@SystemControllerLog(description = "批量导入流转库存")
	@RequiresPermissions(value = "inventoryTransfer:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/inventoryTransfers");
		String error = this.inventoryTransferService.upload(request, session);
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
	@RequestMapping(value = "/inventoryTransfer/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.inventoryTransferService.findproInfo(request);
	}
	
	
	@RequestMapping(value = "/inventoryTransfer/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag,HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		return this.columnService.editColumns("inventoryTransfer", column, flag,user.getId());
	}
	
	
	@RequestMapping(value = "/inventoryTransfer/getSupplier", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getSupplier(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.supplierService.findOneById(id);
	}
	
	
	
	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/inventoryTransfer/export", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportInventoryTransfer (@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end, HttpServletResponse response,
			HttpServletRequest request) throws Exception{
	
		String exportName = Common.fromDateYMD() + "库存流转统计";

		response.setContentType("application/vnd.ms-excel");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		Workbook newExport = this.inventoryTransferService.newExport(request, search, start, end);

		OutputStream out = response.getOutputStream();
		newExport.write(out);
		out.flush();
		out.close();
		
		
		
	}
	
	
	
	
	
	
	

}
