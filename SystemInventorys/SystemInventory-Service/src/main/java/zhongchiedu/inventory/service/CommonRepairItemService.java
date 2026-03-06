package zhongchiedu.inventory.service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.bson.types.ObjectId;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.CommonRepairItem;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.WxReporter;

public interface CommonRepairItemService extends GeneralService<CommonRepairItem> {
	
	/**
	 * 分页查询常见报修项
	 */
	public Pagination<CommonRepairItem> findpagination(Integer pageNo,Integer pageSize,String search);
	
	/**
	 * 保存/更新报修项
	 */
	public void saveOrUpdate(CommonRepairItem commonRepairItem);
	
	/**
	 * 查询所有报修项（区分禁用状态）
	 */
	public List<CommonRepairItem> findAllName(boolean isdisable);

	/**
	 * 批量导入报修项
	 */
	public String BatchImport(File file, int row, HttpSession session);

	/**
	 * 上传导入文件
	 */
	public String upload(HttpServletRequest request, HttpSession session);

	/**
	 * 获取导入进度
	 */
	public ProcessInfo findproInfo(HttpServletRequest request);
	
	/**
	 * 根据搜索词查询ID列表
	 */
	public List<ObjectId> findIdsBySearch(String search);
	
	/**
	 * 获取所有启用的报修项（用于前端快速选择）
	 */
	public List<CommonRepairItem> findAllEnabledItems();
	
	
	
	public String delete(String id);
	

	public List<CommonRepairItem> listByDeviceName(String deviceName);
	
}