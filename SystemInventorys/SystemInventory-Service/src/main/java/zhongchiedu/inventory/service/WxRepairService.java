package zhongchiedu.inventory.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.WxRepair;

public interface WxRepairService extends GeneralService<WxRepair> {

	public Pagination<WxRepair> findpagination(Integer pageNo, Integer pageSize, String search, Integer status,
			String urgencyLevel, String workerId);

	public void saveOrUpdate(WxRepair wxRepair, MultipartFile[] photos, String imgPath, String dir);

	public String delete(String id);

	public List<WxRepair> findWxRepairByOpenId(String openId);

	public WxRepair assignWorkerToRepair(String repairId, String id, String projectId);

	public WxRepair confirmRepair(String repairId);

	public List<WxRepair> findOperationsWxRepairByOpenId(String openId, String search, Integer status, String workerId);

	public boolean completeRepair(WxRepair wxRepair, MultipartFile[] repairPhotos, String imgPath, String dir);

	List<WxRepair> findExportData(String startTime, String endTime, String workerId, Integer status, String search);

	WxRepair cancelAssign(String repairId);

	public WxRepair cancelRepair(String repairId);

	Pagination<WxRepair> findOperationsWxRepairByOpenIdWithPage(String openId, String search, Integer status,
			String workerId, Integer pageNo, Integer pageSize);

	public WxRepair cancelRepairToComplete(String repairId, String workDept);

	void batchInsert(List<WxRepair> list);

	/**
	 * 获取导入进度
	 */
	public ProcessInfo findproInfo(HttpServletRequest request);

	String upload(HttpServletRequest request, HttpSession session);

	boolean completeRepairWithTriplicate(WxRepair wxRepair, MultipartFile[] repairPhotos,
			MultipartFile[] triplicatePhotos, // 三联单
			String imgPath, String dir);

}
