package zhongchiedu.inventory.service;

import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.PickUpApplication;

public interface PickUpApplicationService extends GeneralService<PickUpApplication> {
	
	public Pagination<PickUpApplication> findpagination(Integer pageNo,Integer pageSize,String search,String searchArea,String status);
	
	public void saveOrUpdate(PickUpApplication stock);
	
	public BasicDataResult disable(String id);
	
	public List<PickUpApplication> findAllPickUpApplication(boolean isdisable,String areaId);
	
	public String delete(String id);
	
	public BasicDataResult todisable(String id);
	
	public List<PickUpApplication> findAllPickUpApplicationByStatus(boolean isdisable,int status);
	
	public List<PickUpApplication> findPickUpApplicationsByStockId(String stockId);
	
	
	
}
