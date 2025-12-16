package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.bson.types.ObjectId;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.AfterSalesProjects;
import zhongchiedu.inventory.pojo.ProcessInfo;


public interface AfterSalesProjectsService extends GeneralService<AfterSalesProjects> {
	
	public Pagination<AfterSalesProjects> findpagination(Integer pageNo,Integer pageSize,String search);
	
	public void saveOrUpdate(AfterSalesProjects aAfterSalesProjects);
	
	public List<AfterSalesProjects> findAllName(boolean isdisable);

	public String BatchImport(File file, int row, HttpSession session);

	public String upload(HttpServletRequest request, HttpSession session);

	public ProcessInfo findproInfo(HttpServletRequest request);
	
	public List<AfterSalesProjects> findAllinServiceProj();

	public List<ObjectId> findIdsBySearch(String search);
	
	
	
	
	
	
	
	
}
