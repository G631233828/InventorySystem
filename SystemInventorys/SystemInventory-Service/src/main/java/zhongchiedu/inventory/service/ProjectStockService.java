package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.Supplier;

public interface ProjectStockService extends GeneralService<ProjectStock> {
	
	public Pagination<ProjectStock> findpagination(Integer pageNo,Integer pageSize,String search,String projectName,String searchArea);
	
	public void saveOrUpdate(ProjectStock stock,String areaId);
	
	
	public List<ProjectStock> findAllProjectStock(boolean isdisable,String areaId);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String projectName,String name,String model);
	
	public BasicDataResult todisable(String id);
	
	public ProcessInfo findproInfo(HttpServletRequest request);
	
	public String BatchImport(File file, int row, HttpSession session);
	
	public String upload( HttpServletRequest request, HttpSession session);
	
	public ProjectStock findByName(String name,String model,String projectName,Supplier supplier);
	
	public BasicDataResult findOneById(String id);
	
	public HSSFWorkbook export(String name,String areaId);
	
	public List<ProjectStock> findLowProjectStock(int num);
	
	public Set findProjectNames();
	
	
	
}
