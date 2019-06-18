package zhongchiedu.general.service;

import java.util.List;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.log.pojo.Log;

public interface LogService  extends GeneralService<Log>{

	public Pagination<Log> findAllLog(int pageNo,int pageSize,String type);
	
	
}
