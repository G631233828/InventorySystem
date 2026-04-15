package zhongchiedu.inventory.service.Impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.Impl.MultiMediaServiceImpl;
import zhongchiedu.inventory.pojo.AttendanceManagement;
import zhongchiedu.inventory.pojo.School;
import zhongchiedu.inventory.service.AttendanceManagementService;

@Service
@Slf4j
public class AttendanceManagementServiceImpl extends GeneralServiceImpl<AttendanceManagement>
		implements AttendanceManagementService {

	@Autowired
	private MultiMediaServiceImpl multiMediaSerice;

	private final Lock lock = new ReentrantLock();

	@Override
	public Pagination<AttendanceManagement> findpagination(Integer pageNo, Integer pageSize, String search, String startDate, String endDate) {
	    Pagination<AttendanceManagement> pagination;
	    try {
	        Query query = new Query();
	        query.addCriteria(Criteria.where("isDelete").is(false));

	        // 1. 姓名搜索
	        if (Common.isNotEmpty(search)) {
	            query.addCriteria(Criteria.where("name").regex(search, "i"));
	        }

	        // 2. 开始日期 >=
	        if (Common.isNotEmpty(startDate)) {
	            query.addCriteria(Criteria.where("signTime").gte(startDate));
	        }

	        // 3. 结束日期 <=
	        if (Common.isNotEmpty(endDate)) {
	            query.addCriteria(Criteria.where("signTime").lte(endDate));
	        }

	        // 排序
	        query.with(Sort.by(Sort.Direction.DESC, "createTime"));

	        // 分页查询
	        pagination = this.findPaginationByQuery(query, pageNo, pageSize, AttendanceManagement.class);

	        if (pagination == null) {
	            pagination = new Pagination<>();
	        }
	    } catch (Exception e) {
	        log.error("查询签到记录失败", e);
	        return new Pagination<>();
	    }
	    return pagination;
	}

	@Override
	public void saveOrUpdate(AttendanceManagement attendance, MultipartFile[] photos, String imgPath, String dir) {
		if (photos != null && photos.length > 0) {
			List<MultiMedia> uploadPictures = this.multiMediaSerice.uploadPictures(photos, dir, imgPath, "ATTENDANCE");
			if (!uploadPictures.isEmpty()) {
				attendance.setPhotos(uploadPictures);
			}
		}

		if (Common.isNotEmpty(attendance)) {
			if (Common.isNotEmpty(attendance.getId())) {
				AttendanceManagement old = this.findOneById(attendance.getId(), AttendanceManagement.class);
				BeanUtils.copyProperties(attendance, old);
				this.save(old);
			} else {
				this.insert(attendance);
			}
		}
	}

	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String aid : ids) {
				AttendanceManagement attendance = this.findOneById(aid, AttendanceManagement.class);
				if (attendance != null) {
					attendance.setIsDelete(true);
					this.save(attendance);
				}
			}
			return "success";
		} catch (Exception e) {
			log.error("删除签到记录失败", e);
			return "error";
		} finally {
			lock.unlock();
		}
	}

	@Override
	public List<AttendanceManagement> findExportData(String startTime, String endTime, String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));

		// 1. 日期范围
		if (Common.isNotEmpty(startTime) && Common.isNotEmpty(endTime)) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				Date start = sdf.parse(startTime);
				Date end = sdf.parse(endTime);

				Calendar cal = Calendar.getInstance();
				cal.setTime(end);
				cal.add(Calendar.DATE, 1);
				end = cal.getTime();

				query.addCriteria(Criteria.where("createTime").gte(start).lt(end));
			} catch (ParseException e) {
				log.error("日期解析失败", e);
			}
		}

		// 2. 姓名搜索
		if (Common.isNotEmpty(name)) {
			query.addCriteria(Criteria.where("name").regex(name, "i"));
		}

		// 排序
		query.with(Sort.by(Sort.Direction.DESC, "createTime"));

		return this.find(query, AttendanceManagement.class);
	}

	@Override
	public void saveSign(AttendanceManagement attendance, MultipartFile[] photos, String imgPath, String dir) {
		// ====================== 完全参照你的报修图片上传 ======================
		// 1. 上传照片（和你 WxRepairServiceImpl 一模一样）
		if (photos != null && photos.length > 0) {
			List<MultiMedia> uploadPictures = this.multiMediaSerice.uploadPictures(photos, dir, imgPath, "ATTENDANCE");
			if (uploadPictures != null && uploadPictures.size() > 0) {
				// 把上传后的图片列表 set 进签到实体
				attendance.setPhotos(uploadPictures);
			}
		}

		// 2. 保存签到（新增/更新，和你报修写法一致）
		if (Common.isNotEmpty(attendance)) {
			if (Common.isNotEmpty(attendance.getId())) {
				// 更新
				AttendanceManagement old = this.findOneById(attendance.getId(), AttendanceManagement.class);
				BeanUtils.copyProperties(attendance, old);
				this.save(old);
			} else {
				// 新增
				this.insert(attendance);
			}
		}
	}

	@Override
	public AttendanceManagement getTodaySignRecord(String openId) {
		// 今天 00:00:00
		Calendar startCal = Calendar.getInstance();
		startCal.set(Calendar.HOUR_OF_DAY, 0);
		startCal.set(Calendar.MINUTE, 0);
		startCal.set(Calendar.SECOND, 0);
		startCal.set(Calendar.MILLISECOND, 0);
		Date start = startCal.getTime();

		// 今天 23:59:59
		Calendar endCal = Calendar.getInstance();
		endCal.set(Calendar.HOUR_OF_DAY, 23);
		endCal.set(Calendar.MINUTE, 59);
		endCal.set(Calendar.SECOND, 59);
		endCal.set(Calendar.MILLISECOND, 999);
		Date end = endCal.getTime();

		Query query = new Query();
		query.addCriteria(Criteria.where("openId").is(openId));
		query.addCriteria(Criteria.where("isDisable").is(false));	
		query.addCriteria(Criteria.where("isDelete").is(false));	
		query.addCriteria(Criteria.where("createTime").gte(start).lte(end));

		// 只查一次！
		return this.findOneByQuery(query, AttendanceManagement.class);
	}

	@Override
	public void updateTodayProblem(String openId, String content) {
		// 获取今天 00:00:00
		Calendar startCal = Calendar.getInstance();
		startCal.set(Calendar.HOUR_OF_DAY, 0);
		startCal.set(Calendar.MINUTE, 0);
		startCal.set(Calendar.SECOND, 0);
		startCal.set(Calendar.MILLISECOND, 0);
		Date start = startCal.getTime();

		// 获取今天 23:59:59
		Calendar endCal = Calendar.getInstance();
		endCal.set(Calendar.HOUR_OF_DAY, 23);
		endCal.set(Calendar.MINUTE, 59);
		endCal.set(Calendar.SECOND, 59);
		endCal.set(Calendar.MILLISECOND, 999);
		Date end = endCal.getTime();

		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));	
		query.addCriteria(Criteria.where("openId").is(openId));
		query.addCriteria(Criteria.where("createTime").gte(start).lte(end));

		Update update = new Update();
		update.set("problems", content);

		this.updateFirst(query, update, AttendanceManagement.class);
	}

}