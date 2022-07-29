package zhongchiedu.inventory.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;

public interface StockStatisticsService extends GeneralService<StockStatistics> {
	
	
	public Pagination<StockStatistics> findpagination(Integer pageNo,Integer pageSize,String search,String start,String end,String type,String id,String searchArea,String searchAgent);
	
	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics,User user);
	
	public long updateStock(Stock stock,long num,boolean inOrOut);
	
	public BasicDataResult revoke(String id);
	
	public HSSFWorkbook export(String search,String start,String end,String type,String name,String areaId,String searchAgent);
	
	public List<StockStatistics> findAllByDate(String date,boolean inOrOut);
	
	public List<StockStatistics> findAllStockStatics();
	
	public byte[] exportWord(String id, HttpServletRequest request,HttpSession session);
	
	public List<StockStatistics> findByoutboundOrder(String outboundOrder);
	
	public StockStatistics createStockStatisticsQrCodeAndDownload(String id);
	
	public Map<Object,Object> stockStatisticsPickup(StockStatistics stockStatistics);
	
	
	
	
	
	
}
