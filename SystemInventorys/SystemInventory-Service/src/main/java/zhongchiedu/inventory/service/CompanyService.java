package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Companys;

public interface CompanyService extends GeneralService<Companys> {
	
	public Pagination<Companys> findpagination(Integer pageNo,Integer pageSize);
	
	public void saveOrUpdate(Companys company);
	
	
	public List<Companys> findAllCompany(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name);
	
	public BasicDataResult todisable(String id);
	

}
