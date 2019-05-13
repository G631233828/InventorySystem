package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.pojo.GoodsStorage;

public interface GoodsStorageService extends GeneralService<GoodsStorage> {
	
	public Pagination<GoodsStorage> findpagination(Integer pageNo,Integer pageSize);
	
	public void saveOrUpdate(GoodsStorage goodsStorage);
	
	public BasicDataResult disable(String id);
	
	public List<GoodsStorage> findAllGoodsStorage(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String address,String shelfNumber,String shelflevel);
	
	public BasicDataResult todisable(String id);
	

}
