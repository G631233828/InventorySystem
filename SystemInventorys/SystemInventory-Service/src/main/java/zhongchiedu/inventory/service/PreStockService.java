package zhongchiedu.inventory.service;

import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.Stock;

public interface PreStockService extends GeneralService<PreStock> {
	
	public Pagination<PreStock> findpagination(Integer pageNo,Integer pageSize,String search,String searchArea,int status);
	
	public void saveOrUpdate(PreStock stock);
	
	public List<PreStock> findAllPreStock(boolean isdisable,String areaId);
	
	public String delete(String id);
	
	
	public BasicDataResult todisable(String id);
	
	public PreStock findByName(String name,String model);
	
	public BasicDataResult findOneById(String id);
	
	public HSSFWorkbook export(String name,String areaId);
	
	public List<PreStock> findAllPreStockByStatus(boolean isdisable,int status);
	
	
	
	
}