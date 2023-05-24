package zhongchiedu.inventory.service;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.NewCustomer;
import zhongchiedu.inventory.pojo.ProcessInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.List;


public interface NewCustomerService extends GeneralService<NewCustomer> {
	
	public Pagination<NewCustomer> findpagination(Integer pageNo,Integer pageSize,String search);
	
	public void saveOrUpdate(NewCustomer newCustomer);
	
	public List<NewCustomer> findAllCustomer(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name);

	public BasicDataResult ajaxgetCustomer(String abs);

	public BasicDataResult todisable(String id);
	
	public NewCustomer findByName(String name);
	
	public List findIdsByName(String name);

	public String BatchImport(File file, int row, HttpSession session);

	public String upload(HttpServletRequest request, HttpSession session);


	public ProcessInfo findproInfo(HttpServletRequest request);

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}