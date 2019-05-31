package zhongchiedu.controller.inventory;

import javax.servlet.http.HttpSession;

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
import zhongchiedu.inventory.pojo.StockStatistics;
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

	@GetMapping("stockStatisticss")
	@RequiresPermissions(value = "stockStatistics:list")
	@SystemControllerLog(description = "查询所有库存管理")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session) {


		return "admin/stockStatistics/list";
	}

	

	@RequestMapping(value = "/stockStatistics/in", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
//	@RequiresPermissions(value = "stockStatistics:in")
	@SystemControllerLog(description = "商品入库")
	public BasicDataResult in(@ModelAttribute("stockStatistics") StockStatistics stockStatistics,HttpSession session) {
		System.out.println(123);
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, session);
	}

	@RequestMapping(value = "/stockStatistics/out", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatisticss:out")
	@SystemControllerLog(description = "商品出库")
	public BasicDataResult out(@ModelAttribute("stockStatisticss") StockStatistics stockStatistics,HttpSession session) {
	
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, session);
	
	}
	
	@RequestMapping(value = "/stockStatistics/findStock", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getStock(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.stockService.findOneById(id);
	}

	
	
	
	


}
