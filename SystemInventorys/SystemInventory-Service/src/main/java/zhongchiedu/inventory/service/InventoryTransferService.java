package zhongchiedu.inventory.service;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.InventoryTransfer;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;

public interface InventoryTransferService extends GeneralService<InventoryTransfer> {

	public Pagination<InventoryTransfer> findpagination(Integer pageNo,Integer pageSize,String start, String end,String search);
	
	public void saveOrUpdate(InventoryTransfer inventoryTransfer);
	
	public String delete(String id);
	
	public BasicDataResult todisable(String id);
	
	public ProcessInfo findproInfo(HttpServletRequest request);
	
	public String BatchImport(File file, int row, HttpSession session);
	
	public String upload( HttpServletRequest request, HttpSession session);
	
	public HSSFWorkbook export(String name,String areaId,String searchAgent);
	
	public Workbook newExport(HttpServletRequest request ,String search, String start, String end);
	
	public List<InventoryTransfer> findInventoryTransfers(String search, String start, String end);
	
	

	
}
