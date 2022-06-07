package zhongchiedu.controller.inventory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.ProjectStockStatistics;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.ProjectStockServiceImpl;
import zhongchiedu.inventory.service.Impl.ProjectStockStatisticsServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class ProjectStockStatisticsController {

	private @Autowired ProjectStockStatisticsServiceImpl projectStockStatisticsService;

	private @Autowired ProjectStockServiceImpl projectStockService;

	private @Autowired ColumnServiceImpl columnService;
	
	private @Autowired AreaServiceImpl areaService;

	@GetMapping("projectStockStatisticss")
	@RequiresPermissions(value = "projectStockStatistics:list")
	@SystemControllerLog(description = "查询项目库存统计")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end,
			@RequestParam(value = "type", defaultValue = "") String type,
			@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea) {

		Pagination<ProjectStockStatistics> pagination = this.projectStockStatisticsService.findpagination(pageNo,
				pageSize, search, start, end, type, id,searchArea);
		model.addAttribute("pageList", pagination);
		List<String> listColums = this.columnService.findColumns("projectStockStatistics");
		model.addAttribute("listColums", listColums);
		model.addAttribute("search", search);
		model.addAttribute("start", start);
		model.addAttribute("end", end);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("id", id);
		model.addAttribute("type", type);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		model.addAttribute("searchArea", searchArea);
		return "admin/projectStockStatistics/list";
	}

	@RequestMapping(value = "/projectStockStatistics/in", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "projectStockStatistics:in")
	@SystemControllerLog(description = "项目设备入库")
	public synchronized BasicDataResult in(@ModelAttribute("projectStockStatistics") ProjectStockStatistics projectStockStatistics,
			HttpSession session) {
		 User user =(User)session.getAttribute(Contents.USER_SESSION);
		return this.projectStockStatisticsService.inOrOutstockStatistics(projectStockStatistics, user);
	}

	@RequestMapping(value = "/projectStockStatistics/out", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "projectStockStatistics:out")
	@SystemControllerLog(description = "项目设备出库")
	public synchronized BasicDataResult out(@ModelAttribute("projectStockStatistics") ProjectStockStatistics projectStockStatistics,
			HttpSession session) {
	 User user =(User)session.getAttribute(Contents.USER_SESSION);
		return this.projectStockStatisticsService.inOrOutstockStatistics(projectStockStatistics,user);

	}

	@RequestMapping(value = "/projectStockStatistics/findProjectStock", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getStock(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.projectStockService.findOneById(id);
	}

	@RequestMapping(value = "/projectStockStatistics/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag) {
		return this.columnService.editColumns("projectStockStatistics", column, flag);
	}

	@RequestMapping(value = "/projectStockStatistics/revoke", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "项目出入库撤销")
	@RequiresPermissions(value = "projectStockStatistics:revoke")
	public BasicDataResult revoke(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.projectStockStatisticsService.revoke(id);
	}

	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/projectStockStatistics/export", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportStock(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end,
			@RequestParam(value = "type", defaultValue = "") String type,
			@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "areaId", defaultValue = "") String areaId,
			HttpServletResponse response) {
		try {

			String name = "";
			if (type.equals("in")) {
				name = "项目入库统计记录";
			} else if (type.equals("out")) {
				name = "项目出库统计记录";
			} else {
				name = "项目出库入库统计记录";
			}
			String exportName = Common.fromDateYMD() + name;

			HSSFWorkbook wb = this.projectStockStatisticsService.export(search, start, end, type, exportName,areaId);
			response.setContentType("application/vnd.ms-excel");
			String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
			OutputStream ouputStream = response.getOutputStream();
			wb.write(ouputStream);
			ouputStream.flush();
			ouputStream.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
