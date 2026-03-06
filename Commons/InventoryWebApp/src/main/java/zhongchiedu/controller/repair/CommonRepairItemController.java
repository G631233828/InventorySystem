package zhongchiedu.controller.repair;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.CommonRepairItem;
import zhongchiedu.inventory.service.CommonRepairItemService;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 常见报修项 Controller
 */
@Controller
@Slf4j
public class CommonRepairItemController {

	@Autowired
	private CommonRepairItemService commonRepairItemService;

	/**
	 * 常见报修项列表页
	 */
	@GetMapping("commonRepairItems")
	@RequiresPermissions(value = "commonRepairItems:list")
	@SystemControllerLog(description = "查询所有常见报修项信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "search", defaultValue = "") String search) {
		model.addAttribute("errorImport", errorImport);
		Pagination<CommonRepairItem> pagination = this.commonRepairItemService.findpagination(pageNo, pageSize, search);
		model.addAttribute("pageList", pagination);
		model.addAttribute("search", search);
		return "admin/commonRepairItem/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/commonRepairItem")
	@RequiresPermissions(value = "commonRepairItems:add")
	public String addPage(Model model) {
		return "admin/commonRepairItem/add";
	}

	/**
	 * 新增报修项提交
	 */
	@PostMapping("/commonRepairItem")
	@RequiresPermissions(value = "commonRepairItems:add")
	@SystemControllerLog(description = "添加常见报修项信息")
	public String addCommonRepairItem(@ModelAttribute("commonRepairItem") CommonRepairItem commonRepairItem) {
		this.commonRepairItemService.saveOrUpdate(commonRepairItem);
		return "redirect:commonRepairItems";
	}

	/**
	 * 编辑报修项提交
	 */
	@PutMapping("/commonRepairItem")
	@RequiresPermissions(value = "commonRepairItems:edit")
	@SystemControllerLog(description = "修改常见报修项信息")
	public String edit(@ModelAttribute("commonRepairItem") CommonRepairItem commonRepairItem) {
		this.commonRepairItemService.saveOrUpdate(commonRepairItem);
		return "redirect:commonRepairItems";
	}

	/**
	 * 跳转到编辑页面
	 */
	@GetMapping("/commonRepairItem{id}")
	@RequiresPermissions(value = "commonRepairItems:edit")
	@SystemControllerLog(description = "编辑常见报修项信息")
	public String toeditPage(@PathVariable String id, Model model) {
		CommonRepairItem commonRepairItem = this.commonRepairItemService.findOneById(id, CommonRepairItem.class);
		model.addAttribute("commonRepairItem", commonRepairItem);
		return "admin/commonRepairItem/add";
	}

	/**
	 * 导入模板下载
	 */
	@RequestMapping(value = "/commonRepairItem/download")
	@SystemControllerLog(description = "下载常见报修项导入模版")
	@RequiresPermissions(value = "commonRepairItems:batch")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "常见报修项模版.xlsx";
		String contentType = "application/octet-stream";
		String UPLOAD = "Templates/";
		FileOperateUtil.download(request, response, storeName, contentType, UPLOAD);
		return null;
	}

	/**
	 * Excel批量上传导入
	 */
	@RequestMapping(value = "/commonRepairItem/upload")
	@SystemControllerLog(description = "批量导入常见报修项信息")
	@RequiresPermissions(value = "commonRepairItems:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传常见报修项文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/commonRepairItems");
		String error = this.commonRepairItemService.upload(request, session);
		attr.addFlashAttribute("errorImport", error);
		return modelAndView;
	}

	/**
	 * 获取导入进度
	 */
	@RequestMapping(value = "/commonRepairItem/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.commonRepairItemService.findproInfo(request);
	}

	/**
	 * 禁用/启用报修项
	 */
	@RequestMapping(value = "/commonRepairItem/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "禁用/启用常见报修项信息")
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		CommonRepairItem item = this.commonRepairItemService.findOneById(id, CommonRepairItem.class);
		if (item == null) {
			return BasicDataResult.build(400, "未找到该报修项信息", null);
		}
		boolean newStatus = !item.getIsDisable();
		item.setIsDisable(newStatus);
		this.commonRepairItemService.save(item);
		return BasicDataResult.build(200, newStatus ? "禁用成功" : "启用成功", newStatus);
	}

	/**
	 * 删除报修项（逻辑删除）
	 */
	@DeleteMapping("/commonRepairItem/{id}")
	@RequiresPermissions(value = "commonRepairItems:delete")
	@SystemControllerLog(description = "删除常见报修项信息")
	public String delete(@PathVariable String id) {
		log.info("删除常见报修项信息：" + id);
		this.commonRepairItemService.delete(id);
		log.info("删除常见报修项信息：" + id + "成功");
		return "redirect:/commonRepairItems";
	}

	/**
	 * 获取所有启用的报修项（用于前端快速选择）
	 */
	@RequestMapping(value = "/commonRepairItem/getAllEnabledItems", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "获取所有启用的常见报修项")
	public BasicDataResult getAllEnabledItems() {
		List<CommonRepairItem> items = this.commonRepairItemService.findAllEnabledItems();
		if (items.size() > 0) {
			return BasicDataResult.build(200, "获取报修项成功", items);
		}
		return BasicDataResult.build(400, "获取报修项失败", null);
	}

}