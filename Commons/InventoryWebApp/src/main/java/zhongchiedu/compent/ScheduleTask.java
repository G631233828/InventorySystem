package zhongchiedu.compent;

import java.time.LocalDate;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.inventory.service.MonthEndStatisticsService;

@Slf4j
@Component
public class ScheduleTask {

	@Autowired
	private MonthEndStatisticsService monthEndStatisticsService;

//	@Scheduled(cron = "0 */2 * * * ?")//测试 2分钟执行一次
	@Scheduled(cron = "0 59 23 28-31 * ?")
	public void todoSchedule() {
		final Calendar c = Calendar.getInstance();
		System.out.println("执行库存月末统计，判断是否最后一天");
		if (c.get(Calendar.DATE) == c.getActualMaximum(Calendar.DATE)) {
			System.out.println("执行库存月末统计");
			// 是最后一天
			log.info("开始统计数据" + LocalDate.now().toString());
			this.monthEndStatisticsService.automaticStatistics();
			log.info("统计完成");
		}

	}

}
