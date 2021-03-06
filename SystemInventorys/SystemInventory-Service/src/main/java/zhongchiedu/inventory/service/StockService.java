package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;

public interface StockService extends GeneralService<Stock> {
	
	public Pagination<Stock> findpagination(Integer pageNo,Integer pageSize,String search,String searchArea);
	
	public void saveOrUpdate(Stock stock);
	
	public BasicDataResult disable(String id);
	
	public List<Stock> findAllStock(boolean isdisable,String areaId);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name,String areaId,String model);
	
	public BasicDataResult todisable(String id);
	
	public ProcessInfo findproInfo(HttpServletRequest request);
	
	public String BatchImport(File file, int row, HttpSession session);
	
	public String upload( HttpServletRequest request, HttpSession session);
	
	public Stock findByName(String name,String model);
	
	public BasicDataResult findOneById(String id);
	
	public HSSFWorkbook export(String name,String areaId);
	
	public List<Stock> findLowStock(int num);
	
	
	
}
