package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.general.pojo.Resource;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.pojo.GoodsStorage;

public interface GoodsStorageService extends GeneralService<GoodsStorage> {
	
	public Pagination<GoodsStorage> findpagination(Integer pageNo,Integer pageSize,String searchArea,String search);
	
	public void saveOrUpdate(GoodsStorage goodsStorage);
	
	
	public List<GoodsStorage> findAllGoodsStorage(boolean isdisable,String areaId);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String address,String shelfNumber,String shelflevel);
	
	public BasicDataResult todisable(String id);
	
	public List<GoodsStorage> findStorages(String areaId);
}
