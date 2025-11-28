package zhongchiedu.inventory.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.WxRepair;

public interface WxRepairService extends GeneralService<WxRepair> {
	

	public Pagination<WxRepair> findpagination(Integer pageNo,Integer pageSize);
	
	public void saveOrUpdate(WxRepair wxRepair, MultipartFile[] photos, String imgPath, String dir);
	
	public String delete(String id);
	
	public List<WxRepair> findWxRepairByOpenId(String openId);

	public WxRepair assignWorkerToRepair(String repairId, String id);

	public WxRepair confirmRepair(String repairId);

	public List<WxRepair> findOperationsWxRepairByOpenId(String openId,String search,Integer status,String workerId);

	public boolean completeRepair(WxRepair wxRepair, MultipartFile[] repairPhotos, String imgPath, String dir);
	
	
	
	
	
	
	
	
	
}
