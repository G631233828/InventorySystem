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
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class StockStatisticsController {

	private @Autowired StockStatisticsServiceImpl stockStatisticsService;

	private @Autowired StockServiceImpl stockService;

	private @Autowired ColumnServiceImpl columnService;

	private @Autowired AreaServiceImpl areaService;

	@GetMapping("stockStatisticss")
	@RequiresPermissions(value = "stockStatistics:list")
	@SystemControllerLog(description = "查询库存统计")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end,
			@RequestParam(value = "type", defaultValue = "") String type,
			@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea
			) {
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		Pagination<StockStatistics> pagination = this.stockStatisticsService.findpagination(pageNo, pageSize, search,
				start, end, type, id,searchArea);
		model.addAttribute("pageList", pagination);
		List<String> listColums = this.columnService.findColumns("stockStatistics");
		model.addAttribute("listColums", listColums);
		model.addAttribute("search", search);
		model.addAttribute("start", start);
		model.addAttribute("end", end);
		model.addAttribute("id", id);
		model.addAttribute("type", type);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("searchArea", searchArea);
		return "admin/stockStatistics/list";
	}

	@RequestMapping(value = "/stockStatistics/in", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:in")
	@SystemControllerLog(description = "设备入库")
	public BasicDataResult in(@ModelAttribute("stockStatistics") StockStatistics stockStatistics, HttpSession session) {
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, session);
	}

	@RequestMapping(value = "/stockStatistics/out", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:out")
	@SystemControllerLog(description = "设备出库")
	public BasicDataResult out(@ModelAttribute("stockStatistics") StockStatistics stockStatistics,
			HttpSession session) {
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, session);

	}

	@RequestMapping(value = "/stockStatistics/findStock", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getStock(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.stockService.findOneById(id);
	}

	@RequestMapping(value = "/stockStatistics/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag) {
		return this.columnService.editColumns("stockStatistics", column, flag);
	}

	@RequestMapping(value = "/stockStatistics/revoke", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "出入库撤销")
	@RequiresPermissions(value = "stockStatistics:revoke")
	public BasicDataResult revoke(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.stockStatisticsService.revoke(id);
	}

	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/stockStatistics/export", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
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
				name = "入库统计记录";
			} else if (type.equals("out")) {
				name = "出库统计记录";
			} else {
				name = "出库入库统计记录";
			}
			String exportName = Common.fromDateYMD() + name;

			HSSFWorkbook wb = this.stockStatisticsService.export(search, start, end, type, exportName,areaId);
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
