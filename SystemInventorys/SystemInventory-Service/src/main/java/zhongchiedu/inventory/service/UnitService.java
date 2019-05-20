package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Unit;

public interface UnitService extends GeneralService<Unit> {
	
	public Pagination<Unit> findpagination(Integer pageNo,Integer pageSize);
	
	public void saveOrUpdate(Unit unit);
	
	public BasicDataResult disable(String id);
	
	public List<Unit> findAllUnit(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name);
	
	public BasicDataResult todisable(String id);
	
	public ProcessInfo findproInfo(HttpServletRequest request);
	
	public String BatchImport(File file, int row, HttpSession session);
	
	public String upload( HttpServletRequest request, HttpSession session);
	
	public Unit findByName(String name);
	
	
	
	
	
	
	
	
	
	
	
	
}
