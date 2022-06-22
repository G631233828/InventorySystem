package zhongchiedu.controller.inventory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
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
import zhongchiedu.common.utils.WordUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Stock;
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
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea,
			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent
			) {
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		Pagination<StockStatistics> pagination = this.stockStatisticsService.findpagination(pageNo, pageSize, search,
				start, end, type, id,searchArea,searchAgent);
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
		model.addAttribute("searchAgent", searchAgent);
		return "admin/stockStatistics/list";
	}

	@RequestMapping(value = "/stockStatistics/in", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:in")
	@SystemControllerLog(description = "设备入库")
	public BasicDataResult in(@ModelAttribute("stockStatistics") StockStatistics stockStatistics, HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
	}

	@RequestMapping(value = "/stockStatistics/out", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:out")
	@SystemControllerLog(description = "设备出库")
	public BasicDataResult out(@ModelAttribute("stockStatistics") StockStatistics stockStatistics,
			HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		String orderNum = Common.getOrderNum();
		stockStatistics.setOutboundOrder(orderNum);
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);

	}
	
	@RequestMapping(value = "/stockStatistics/batchOut", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:out")
	@SystemControllerLog(description = "设备出库")
	public BasicDataResult batchOut(String batchid,String batchnum, String batchdescription,String batchpersonInCharge,String batchprojectName,String batchcustomer,
			HttpSession session) {
	
		
		
		String[] ids = batchid.split(",");
		String[] nums = batchnum.split(",");
		List<String> batchidList = Arrays.asList(ids);
		List<String> batchnumList = Arrays.asList(nums);
		
		if(batchidList.size()!=batchnumList.size()) {
			return new BasicDataResult(400, "出库商品与id不匹配", "");
		}
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		String orderNum = Common.getOrderNum();
		List<Object> list = new ArrayList<>();
		
		for(int i=0;i<batchidList.size();i++) {
			Stock stock = this.stockService.findOneById(batchidList.get(i), Stock.class);
			StockStatistics st= new StockStatistics();
			st.setStock(stock);
			st.setNum(Long.valueOf(batchnumList.get(i)));
			st.setPersonInCharge(batchpersonInCharge);
			st.setProjectName(batchprojectName);
			st.setCustomer(batchcustomer);
			st.setDescription(batchdescription);
			st.setInOrOut(false);
			st.setOutboundOrder(orderNum);
			BasicDataResult r = this.stockStatisticsService.inOrOutstockStatistics(st, user);
			StockStatistics statics=(StockStatistics) r.getData();
			
			StockStatistics s = new StockStatistics();
			s.setId(statics.getStock().getId());
			s.setNewNum(statics.getNewNum());
			list.add(s);
		}
		//出库成功清除session
		session.removeAttribute(Contents.STOCK_LIST);
	
		 return new BasicDataResult(200, "批量出库成功!", list);
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
			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent,
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

			HSSFWorkbook wb = this.stockStatisticsService.export(search, start, end, type, exportName,areaId,searchAgent);
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
	
	/**
	 * 导出excel
	 * @throws Exception 
	 */
	@RequestMapping(value = "/stockStatistics/exportOutBoundOrder", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportOutBoundOrder(
			@RequestParam(value = "id", defaultValue = "") String id,
			HttpServletRequest request,HttpSession session,
			HttpServletResponse response) throws Exception {
		
		
		
		
		String exportName = Common.fromDateYMD() + "销货单";

			response.setContentType("application/vnd.ms-word;charset=utf-8");
			String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".docx");
			byte[] exportWord = this.stockStatisticsService.exportWord(id, request, session);

			OutputStream out = response.getOutputStream();
			out.write(exportWord);
			out.flush();
			out.close();
			
	}
	
	@RequestMapping(value = "/stockStatistics/update", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "更新数据状态")
	public String updateAgent() {
		
		List<StockStatistics> stocks = this.stockStatisticsService.find(new Query(), StockStatistics.class);
		StringBuffer buf = new StringBuffer();
		stocks.forEach(s->{
			System.out.println(s.getStock()==null);
			if(s.getStock()!=null) {
				System.out.println("修复："+s.getId()+s.getStock().getName()+"数据"+s.isAgent()+"</br>");
				buf.append("修复："+s.getStock().getName()+"数据"+s.isAgent()+"</br>");
				s.setAgent(s.getStock().isAgent());
				this.stockStatisticsService.save(s);
			}
				
		});
		
		
		
		return buf.toString();
	}
	
	
	
	

}
