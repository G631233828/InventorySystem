package zhongchiedu.controller.inventory;

import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.service.MonthEndStatisticsService;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
public class MonthEndStatisticsController {

	@Autowired
	private MonthEndStatisticsService monthEndStatisticsService;

	@GetMapping("/createMonthEndStatistics/{date}")
	@RequiresPermissions(value = "createMonthEndStatistics:create")
	@SystemControllerLog(description = "创建期末数据")
	@ResponseBody
	public BasicDataResult createMonthEndStatistics(@PathVariable String date) {

		try {

			this.monthEndStatisticsService.automaticStatistics(date);
			return BasicDataResult.build(200, "创建成功", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return BasicDataResult.build(400, "创建失败", "");

	}

}
