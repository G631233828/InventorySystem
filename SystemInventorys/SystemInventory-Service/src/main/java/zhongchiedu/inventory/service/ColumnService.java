package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Column;

public interface ColumnService extends GeneralService<Column> {
	
	public List<String> findColumns(String name,String userId);
	
	public BasicDataResult editColumns(String name,String showcolumn,boolean flag,String userId);
	
	
	
	
	
	
	
	
	
}
