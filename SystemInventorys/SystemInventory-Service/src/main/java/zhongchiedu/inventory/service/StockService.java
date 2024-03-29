package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.*;

public interface StockService extends GeneralService<Stock> {
	
	public Pagination<Stock> findpagination(Integer pageNo,Integer pageSize,String search,String searchArea,String searchAgent,String ssC);

	public Pagination<Stock> findpagination(Integer pageNo, Integer pageSize, RequestBo requestBo);
	public void saveOrUpdate(Stock stock);
	
	public void copyStock(String id,HttpSession session);
	
	public List<Stock> findAllStock(boolean isdisable,String areaId,String searchAgent);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name,String areaId,String model);
	
	public BasicDataResult todisable(String id);
	
	public ProcessInfo findproInfo(HttpServletRequest request);
	
	public String BatchImport(File file, int row, HttpSession session);
	
	public String upload( HttpServletRequest request, HttpSession session);


	public Stock findByName(String areaName,String name,String model,String entryName);

	public Stock findByNameSupplier(String areaName,String name,String model,String supplierName);


	public BasicDataResult findOneById(String id);
	
	public HSSFWorkbook export(String name,String areaId,String searchAgent);

	public HSSFWorkbook exportTJ(String name,RequestBo bo);
	
	public List<Stock> findLowStock(int num);
	
	public Set<String> findProjectNames();
	
	public void preStockToStock(PreStock preStock,long actnum);
	
	public BasicDataResult pickUpApplicationToStock(PickUpApplication pickUpApplication);
	
	public Stock findByAreaNameModel(String areaId,String name,String model,String entryName);
	
	public QrCode createStockQrCode(String stockId);
	
	public List<Stock> findStockByIds(String id);
	
	public List<Stock> findStocksByIds(List ids);
	
	//批量采购付款申请单
	public void updateItemNo(String ids,String itemNo);
	
	public List<Stock> findStockByType(String type);
	
	
	
	
	
	
	
	
}
