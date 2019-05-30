package zhongchiedu.inventory.service;

import javax.servlet.http.HttpSession;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;

public interface StockStatisticsService extends GeneralService<StockStatistics> {
	
	
	public Pagination<StockStatistics> findpagination(Integer pageNo,Integer pageSize,boolean inOrOut);
	
	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics,HttpSession session);
	
	public long updateStock(Stock stock,long num,boolean inOrOut);
	
	
}
