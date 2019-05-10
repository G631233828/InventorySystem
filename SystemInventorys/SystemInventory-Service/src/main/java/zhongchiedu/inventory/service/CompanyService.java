package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Companys;

public interface CompanyService extends GeneralService<Companys> {
	
	public void saveOrUpdate(Companys company);
	
	public BasicDataResult disable(String id);
	
	public List<Companys> findAllCompany(boolean isdisable);
	
	public String delete(String id);
	

}
