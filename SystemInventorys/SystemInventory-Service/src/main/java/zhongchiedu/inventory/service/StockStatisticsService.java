package zhongchiedu.inventory.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.NewCustomer;
import zhongchiedu.inventory.pojo.Pname;
import zhongchiedu.inventory.pojo.RequestBo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl.TimeRangeType;

public interface StockStatisticsService extends GeneralService<StockStatistics> {
	
	
	public Pagination<StockStatistics> findpagination(Integer pageNo,Integer pageSize,String search,String start,String end,String type,String id,String searchArea,String searchAgent,String userId,String revoke,String confirm,String ssC);

	public Pagination<StockStatistics> findpagination(Integer pageNo, Integer pageSize, RequestBo requestBo);
	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics,User user);
		
	public Double updateStock(Stock stock,Double num,boolean inOrOut);



	public BasicDataResult revoke(String id,Double num,User user);

	public BasicDataResult confirm(String id);
	
	public HSSFWorkbook export(String search,String start,String end,String type,String name,String areaId,String searchAgent);
	
	public List<StockStatistics> findAllByDate(String date,boolean inOrOut);
	
	public List<StockStatistics> findAllStockStatics();
	
	public byte[] exportWord(String id, HttpServletRequest request,HttpSession session);
	
	public List<StockStatistics> findByoutboundOrder(String outboundOrder);
	
	public StockStatistics createStockStatisticsQrCodeAndDownload(String id);
	
	public Map<Object,Object> stockStatisticsPickup(StockStatistics stockStatistics,String i);
	
	public Workbook newExport( HttpServletRequest request,RequestBo requestBo);

	public Workbook toJD( HttpServletRequest request,RequestBo requestBo);

	public Workbook newExport2( HttpServletRequest request,RequestBo bo);

	public Workbook newExport3( HttpServletRequest request,String search,String start,String end,String type,String name,String areaId,String searchAgent);
	
	public  void updateStockStatistics(String ids,String price,Double inprice,String purchaseInvoiceNo,String receiptNo,String paymentOrderNo,String sailesInvoiceNo,
									   String sailesInvoiceDate,User user,String purchaseInvoiceDate,Double sailPrice,String newItemNo,String description);
	
	public List<StockStatistics> findStockStatisticsToCreateQrcode(Pname pname,NewCustomer newcustomer,String accepter,String depotTime);
	
	public List<StockStatistics> findStockStatisByStockId(String id, TimeRangeType timeRangeType,String date);
	
	 public BigDecimal calculateAveragePriceByStockId(String stockId,TimeRangeType t,String date);
	 
	 public BigDecimal getCurrentMonthInStockNum(String stockId, TimeRangeType t,String date);
	
	 public BigDecimal getQC(String stockId, TimeRangeType t,String date);
}
