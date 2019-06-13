package zhongchiedu.inventory.service;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;

public interface StockStatisticsService extends GeneralService<StockStatistics> {
	
	
	public Pagination<StockStatistics> findpagination(Integer pageNo,Integer pageSize,String search,String start,String end,String type,String id);
	
	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics,HttpSession session);
	
	public long updateStock(Stock stock,long num,boolean inOrOut);
	
	public BasicDataResult revoke(String id);
	
	public HSSFWorkbook export(String search,String start,String end,String type,String name);
	
	public List<StockStatistics> findAllByDate(String date,boolean inOrOut);
	
}
