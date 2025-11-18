package zhongchiedu.inventory.service;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.WxRepair;

public interface WxRepairService extends GeneralService<WxRepair> {
	

	public Pagination<WxRepair> findpagination(Integer pageNo,Integer pageSize);
	
	public void saveOrUpdate(WxRepair wxRepair);
	
	public String delete(String id);
	
	
	
	
	
	
	
	
	
}
