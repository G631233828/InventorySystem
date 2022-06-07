package zhongchiedu.inventory.service;

import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.ProjectStockStatistics;

public interface ProjectStockStatisticsService extends GeneralService<ProjectStockStatistics> {
	
	
	public Pagination<ProjectStockStatistics> findpagination(Integer pageNo,Integer pageSize,String search,String start,String end,String type,String id,String searchArea);
	
	public BasicDataResult inOrOutstockStatistics(ProjectStockStatistics projectStockStatistics,User user);
	
	public Integer updateProjectStock(ProjectStock projectStock,Integer num,boolean inOrOut,boolean revoke);
	
	public BasicDataResult revoke(String id);
	
	public HSSFWorkbook export(String search,String start,String end,String type,String name,String areaId);
	
	public List<ProjectStockStatistics> findAllByDate(String date,boolean inOrOut);
	
}
