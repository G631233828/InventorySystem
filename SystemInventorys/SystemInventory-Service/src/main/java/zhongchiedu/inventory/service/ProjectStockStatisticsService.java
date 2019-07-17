package zhongchiedu.inventory.service;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.ProjectStockStatistics;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;

public interface ProjectStockStatisticsService extends GeneralService<ProjectStockStatistics> {
	
	
	public Pagination<ProjectStockStatistics> findpagination(Integer pageNo,Integer pageSize,String search,String start,String end,String type,String id);
	
	public BasicDataResult inOrOutstockStatistics(ProjectStockStatistics projectStockStatistics,HttpSession session);
	
	public long updateProjectStock(ProjectStock projectStock,long num,boolean inOrOut,boolean revoke);
	
	public BasicDataResult revoke(String id);
	
	public HSSFWorkbook export(String search,String start,String end,String type,String name);
	
	public List<ProjectStockStatistics> findAllByDate(String date,boolean inOrOut);
	
}
