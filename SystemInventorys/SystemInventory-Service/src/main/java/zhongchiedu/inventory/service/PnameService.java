package zhongchiedu.inventory.service;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;

import zhongchiedu.inventory.pojo.Pname;
import zhongchiedu.inventory.pojo.ProcessInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.List;


public interface PnameService extends GeneralService<Pname> {
	
	public Pagination<Pname> findpagination(Integer pageNo,Integer pageSize,String search);
	
	public void saveOrUpdate(Pname pName,String types);
	
	public List<Pname> findAllName(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult checkIfNameExists(String name,String fieldName);

	public BasicDataResult ajaxgetName(String abs);

	public BasicDataResult todisable(String id);
	
	public Pname findByName(String name);
	
	public List findIdsByName(String name);

	public String BatchImport(File file, int row, HttpSession session);

	public String upload(HttpServletRequest request, HttpSession session);


	public ProcessInfo findproInfo(HttpServletRequest request);

	public Object[] newcustomerids(Pname pname);
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
