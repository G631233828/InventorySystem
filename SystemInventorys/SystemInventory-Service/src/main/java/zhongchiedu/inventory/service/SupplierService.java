package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Basic;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Supplier;

public interface SupplierService extends GeneralService<Supplier> {
	
	public Pagination<Supplier> findpagination(Integer pageNo,Integer pageSize, String search);
	
	public void saveOrUpdate(Supplier supplier, String types);
	
	
	public List<Supplier> findAllSupplier(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name);
	
	public BasicDataResult todisable(String id);
	
	public ProcessInfo findproInfo(HttpServletRequest request);
	
	public String BatchImport(File file, int row, HttpSession session);
	
	public String upload( HttpServletRequest request, HttpSession session);
	
	public Supplier findByName(String name);
	
	public List<Supplier> findByRegxName(String name);
	
	public Object[] categorys(Supplier supplier);
	
	public BasicDataResult findOneById(String id);
	
	
	
	
	
	
	
	
	
	
}
