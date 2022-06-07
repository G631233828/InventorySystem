package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.ProjectPickup;
import zhongchiedu.inventory.pojo.Stock;

public interface ProjectPickupService extends GeneralService<ProjectPickup> {
	
	
	public void saveOrUpdate(ProjectPickup projectPickup);
	
	public String delete(String id);
	
	public BasicDataResult todisable(String id);

	public Pagination<ProjectPickup> findpagination(Integer pageNo, Integer pageSize, String search,String start ,String end);
	
	public Map<String, Object> getExportData(String id, String search,String start,String end);
	
	
	
	
}
