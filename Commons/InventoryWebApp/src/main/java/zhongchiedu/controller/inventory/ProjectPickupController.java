package zhongchiedu.controller.inventory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.WordUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.ProjectPickup;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.ProjectPickupService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class ProjectPickupController {

	@Autowired
	private ProjectPickupService projectPickupService;
	
	@Autowired
	private StockStatisticsService stockStatisticsService;
	

	@GetMapping("projectPickups")
	@RequiresPermissions(value = "projectPickup:list")
	@SystemControllerLog(description = "查询取货统计")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end) {

		Pagination<ProjectPickup> pagination = this.projectPickupService.findpagination(pageNo, pageSize, search, start,
				end);
		model.addAttribute("pageList", pagination);
		model.addAttribute("search", search);
		model.addAttribute("start", start);
		model.addAttribute("end", end);
		model.addAttribute("pageSize", pageSize);

		session.setAttribute("start", start);
		session.setAttribute("end", end);
		session.setAttribute("pageNo", pageNo);
		session.setAttribute("pageSize", pageSize);
		session.setAttribute("search", search);

		return "admin/projectPickup/list";
	}

	@DeleteMapping("/projectPickup/{id}")
	@RequiresPermissions(value = "projectPickup:delete")
	@SystemControllerLog(description = "删除取货记录")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String start = (String) session.getAttribute("start");
		String end = (String) session.getAttribute("end");

		log.info("删除项目设备" + id);
		this.projectPickupService.delete(id);
		log.info("删除项目设备" + id + "成功");

		return "redirect:/projectPickups?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&start=" + start + "&end=" + end;

	}

	@RequestMapping("/projectPickup/clearSearch")
//	@RequiresPermissions(value = "projectStock:delete")
	public String clearSearch(HttpSession session) {
		session.removeAttribute("pageNo");
		session.removeAttribute("pageSize");
		session.removeAttribute("search");
		session.removeAttribute("start");
		session.removeAttribute("end");
		return "redirect:/projectPickups";

	}

	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/projectPickup/export")
	@SystemControllerLog(description = "生成取货单")
	public void exportStock(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end) {
		try {
			String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
			Map<String, Object> exportData = this.projectPickupService.getExportData(id, search, start,end);
			response.setContentType("application/msword");
			String name = Common.fromDateYM() + "出库单";
			String fileName = new String((name).getBytes("gb2312"), "ISO8859-1");
			ByteArrayOutputStream output = WordUtil.process(exportData, "出货单.ftl", ctxPath);
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".docx");
			OutputStream ouputStream = response.getOutputStream();
			output.writeTo(ouputStream);
			ouputStream.flush();
			ouputStream.close();
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	@RequestMapping(value = "/projectPickupStockStatistics/in", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:in")
	@SystemControllerLog(description = "一键入库")
	public BasicDataResult in(String id, HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		if(Common.isNotEmpty(id)) {
			ProjectPickup projectPickup = this.projectPickupService.findOneById(id, ProjectPickup.class);
			if(projectPickup!=null) {
				
				if(projectPickup.getStock()==null) {
					return new BasicDataResult(400, "出库过程中遇到问题，当前设备需要进行手动出库", null);
				}
				
				StockStatistics stockStatistics = new StockStatistics();
				stockStatistics.setPersonInCharge(projectPickup.getProjectLeader());
				stockStatistics.setProjectName(projectPickup.getEntryName());
				stockStatistics.setCustomer(projectPickup.getItemNo());
				stockStatistics.setNum(projectPickup.getNum());
				stockStatistics.setInOrOut(false);
				stockStatistics.setStock(projectPickup.getStock());
				BasicDataResult inOrOutstockStatistics = this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
				if(inOrOutstockStatistics.getStatus()==200) {
					//出库成功
					projectPickup.setStatus(true);
					this.projectPickupService.saveOrUpdate(projectPickup);
				}
				return new BasicDataResult(inOrOutstockStatistics.getStatus(), inOrOutstockStatistics.getMsg(), null);
			}
		}
		return new BasicDataResult(400, "出库过程中遇到未知错误", null);
		
		
	}
	

}
