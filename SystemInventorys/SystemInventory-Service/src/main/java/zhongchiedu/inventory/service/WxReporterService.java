package zhongchiedu.inventory.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.pojo.WxReporter;

public interface WxReporterService extends GeneralService<WxReporter> {
	

	public Pagination<WxReporter> findpagination(Integer pageNo,Integer pageSize);
	
	public WxReporter saveOrUpdate(WxReporter w);
	
	public String delete(String id);
	
	public WxReporter findWxReporterByOpenId(String openId);
	
	public List<ObjectId>  findIdsBySearch(String search);
	
	
	
	
	
	
}
