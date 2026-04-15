package zhongchiedu.inventory.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.AttendanceManagement;

public interface AttendanceManagementService extends GeneralService<AttendanceManagement> {

//    Pagination<AttendanceManagement> findpagination(Integer pageNo, Integer pageSize, String search);
    
    Pagination<AttendanceManagement> findpagination(Integer pageNo, Integer pageSize, String name, String startDate, String endDate);

    void saveOrUpdate(AttendanceManagement attendance, MultipartFile[] photos, String imgPath, String dir);

    String delete(String id);
    
 // 导出签到记录
    List<AttendanceManagement> findExportData(String startTime, String endTime, String name);
    
    
    public void saveSign(AttendanceManagement attendance, MultipartFile[] photos, String imgPath, String dir);

    AttendanceManagement  getTodaySignRecord(String openId);
	
	/**
	 * 更新今天的问题汇总
	 */
	void updateTodayProblem(String openId, String content);

}