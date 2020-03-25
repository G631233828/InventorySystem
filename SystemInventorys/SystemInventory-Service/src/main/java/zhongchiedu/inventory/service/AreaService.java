package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Area;

public interface AreaService extends GeneralService<Area> {
	
	public Pagination<Area> findpagination(Integer pageNo,Integer pageSize);
	
	public void saveOrUpdate(Area brand);
	
	public BasicDataResult disable(String id);
	
	public List<Area> findAllArea(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name);
	
	public BasicDataResult todisable(String id);
	
	public Area findByName(String name);
	
	
	
	
	
	
	
	
	
	
	
	
}
