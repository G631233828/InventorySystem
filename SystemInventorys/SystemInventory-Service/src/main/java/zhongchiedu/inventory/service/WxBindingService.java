package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.WxBinding;

public interface WxBindingService extends GeneralService<WxBinding> {
	
	public Pagination<WxBinding> findpagination(Integer pageNo, Integer pageSize);
	
	public void saveOrUpdate(WxBinding wxBinding);
	
	public String delete(String id);
	
	public WxBinding findWxBindingByOpenId(String openId);
	
	
	public List<WxBinding> findBindingsByPersonnelType(PersonnelType p);
	
	
	
	
	
	
	
	
}
