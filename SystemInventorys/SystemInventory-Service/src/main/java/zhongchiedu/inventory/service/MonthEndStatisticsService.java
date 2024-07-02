package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.MonthEndStatistics;

public interface MonthEndStatisticsService extends GeneralService<MonthEndStatistics>{
	
	//自动任务
	 void automaticStatistics(String date);
	 
	 //根据日期获取所有数据
	 List<MonthEndStatistics> findMonthEndStatisticsByDate(String date);

}
