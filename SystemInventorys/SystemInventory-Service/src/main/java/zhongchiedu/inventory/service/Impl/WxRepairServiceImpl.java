package zhongchiedu.inventory.service.Impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.common.utils.enums.RepairStatus;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.Impl.MultiMediaServiceImpl;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.inventory.service.WxReporterService;

@Service
@Slf4j
public class WxRepairServiceImpl extends GeneralServiceImpl<WxRepair> implements WxRepairService {

	@Autowired
	private MultiMediaServiceImpl multiMediaSerice;

	@Autowired
	private WxBindingService wxBindingService;

	@Autowired
	private WxReporterService wxReporterService;

	@Override
	public Pagination<WxRepair> findpagination(Integer pageNo, Integer pageSize, String search, Integer status,
			String urgencyLevel, String workerId) {
		Pagination<WxRepair> pagination = null;
		try {
			Query query = new Query();
			// 基础条件：未删除
			query.addCriteria(Criteria.where("isDelete").is(false));
			// 1. 模糊查询条件（支持工单号、学校、教室、设备等）
			Criteria ca = new Criteria();
			if (Common.isNotEmpty(search)) {
				List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);
				query.addCriteria(ca.orOperator(Criteria.where("workOrderNumber").regex(search, "i"),
						Criteria.where("faultInformation").regex(search, "i"),
						Criteria.where("reportClassroomRepair").regex(search, "i"),
						Criteria.where("equipmentRepair").regex(search, "i"),
						Criteria.where("wxReporter.$id").in(findIdsBySearch)));
			}

			// 2. 维修状态条件
			if (status != null) {
				query.addCriteria(Criteria.where("status").is(status));
			}

			// 3. 紧急程度条件
			if (Common.isNotEmpty(urgencyLevel)) {
				query.addCriteria(Criteria.where("urgencyLevel").is(urgencyLevel));
			}

			// 4. 维修人员条件
			if (Common.isNotEmpty(workerId)) {
				query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
			}

			// 排序：按创建时间降序
			query.with(Sort.by(Sort.Direction.DESC, "createTime"));

			// 分页查询
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, WxRepair.class);
			if (pagination == null) {
				pagination = new Pagination<>();
			}
		} catch (Exception e) {
			log.error("查询报修单列表失败", e);
		}
		return pagination;
	}

	@Override
	public void saveOrUpdate(WxRepair wxRepair, MultipartFile[] photos, String imgPath, String dir) {
		if (photos.length > 0) {
			List<MultiMedia> uploadPictures = this.multiMediaSerice.uploadPictures(photos, dir, imgPath, "WXREPAIR");
			if (uploadPictures.size() > 0) {
				wxRepair.setPhotos(uploadPictures);
			}
		}
		if (Common.isNotEmpty(wxRepair)) {
			if (Common.isNotEmpty(wxRepair.getId())) {
				// update
				WxRepair ed = this.findOneById(wxRepair.getId(), WxRepair.class);
				BeanUtils.copyProperties(wxRepair, ed);
				this.save(wxRepair);
			} else {
				// insert
				this.insert(wxRepair);
			}
		}
	}

	private Lock lock = new ReentrantLock();

	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				WxRepair de = this.findOneById(edid, WxRepair.class);
				de.setIsDelete(true);
				this.save(de);
			}
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return "error";
	}

	@Override
	public List<WxRepair> findWxRepairByOpenId(String openId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		query.addCriteria(Criteria.where("openId").is(openId));
		return this.find(query, WxRepair.class);
	}

	// 根据报修单id 跟拿过来的维修人员（wxbinding）的id来绑定 分配任务给施工队
	@Override
	public WxRepair assignWorkerToRepair(String repairId, String id) {
		WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
		if (wxRepair == null)
			return null;
		WxBinding wxBinding = this.wxBindingService.findOneById(id, WxBinding.class);
		if (wxBinding == null)
			return null;
		wxRepair.setWorker(wxBinding);
		wxRepair.setStatus(RepairStatus.ASSIGNED.getCode());// 修改状态 已分配
		this.save(wxRepair);
		return wxRepair;
	}

	@Override
	public WxRepair confirmRepair(String repairId) {
		WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
		if (wxRepair == null)
			return null;
		wxRepair.setStatus(RepairStatus.PROCESSING.getCode());// 修改状态 已分配
		this.save(wxRepair);
		return wxRepair;
	}

	/**
	 * 维修人员 调度人员查看所有报修信息（不分页）
	 */
	@Override
	public List<WxRepair> findOperationsWxRepairByOpenId(String openId, String search, Integer status,
			String workerId) {
		// 通过openId 去查看opendId所属人员权限
		WxBinding wxBinding = this.wxBindingService.findWxBindingByOpenId(openId);
		PersonnelType personnelType = PersonnelType.getByCode(wxBinding.getPersonnelType())
				.orElseThrow(() -> new IllegalArgumentException("无效的人员类型编码：" + wxBinding.getPersonnelType()));

		Query query = new Query();
		Criteria ca = new Criteria();
		if (Common.isNotEmpty(search)) {
			// 根据search的内容去用户绑定里面查询id
			List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);

			query.addCriteria(ca.orOperator(Criteria.where("workOrderNumber").regex(search, "i"),
					Criteria.where("faultInformation").regex(search, "i"),
					Criteria.where("urgencyLevel").regex(search, "i"),
					Criteria.where("reportClassroomRepair").regex(search, "i"),
					Criteria.where("equipmentRepair").regex(search, "i"),
					Criteria.where("wxReporter.$id").in(findIdsBySearch)));
		}
		if (Common.isNotEmpty(status)) {
			query.addCriteria(Criteria.where("status").is(status));
		}
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		switch (personnelType) {
		case CONSTRUCTION_TEAM:
			if (Common.isEmpty(status)) {
				// 施工队默认查看状态为2的数据
				query.addCriteria(Criteria.where("status").is(2));
			}
			// 维修人员 只能根据自己的openId查看自己的维修数据
			query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(wxBinding.getId())));
			break;
		case DISPATCHER:
			if (Common.isNotEmpty(workerId)) {
				query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
			}
			if (Common.isEmpty(status) && Common.isEmpty(search) && Common.isEmpty(workerId)) {
				// 调度人员默认查看状态为1的数据
				query.addCriteria(Criteria.where("status").is(1));
			}
			break;
		default:
			// 其他类型逻辑
			break;
		}
		return this.find(query, WxRepair.class);
	}

	/**
	 * 分页查询维修工单（适配下拉刷新/滚动加载）
	 */
	@Override
	public Pagination<WxRepair> findOperationsWxRepairByOpenIdWithPage(
	        String openId, String search, Integer status, String workerId, 
	        Integer pageNo, Integer pageSize) {
	    
	    // 1. 获取人员权限
	    WxBinding wxBinding = this.wxBindingService.findWxBindingByOpenId(openId);
	    if (wxBinding == null) {
	        return new Pagination<>();
	    }
	    PersonnelType personnelType = PersonnelType.getByCode(wxBinding.getPersonnelType())
	            .orElseThrow(() -> new IllegalArgumentException("无效的人员类型"));

	    // 2. 构建查询条件
	    Query query = new Query();
	    Criteria ca = new Criteria();
	    
	    // 搜索条件
	    if (Common.isNotEmpty(search)) {
	        List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);
	        query.addCriteria(ca.orOperator(
	            Criteria.where("workOrderNumber").regex(search, "i"),
	            Criteria.where("faultInformation").regex(search, "i"),
	            Criteria.where("urgencyLevel").regex(search, "i"),
	            Criteria.where("reportClassroomRepair").regex(search, "i"),
	            Criteria.where("equipmentRepair").regex(search, "i"),
	            Criteria.where("wxReporter.$id").in(findIdsBySearch)
	        ));
	    }
	    
	    // 状态筛选
	    if (Common.isNotEmpty(status)) {
	        query.addCriteria(Criteria.where("status").is(status));
	    }
	    
	    // 基础条件：未删除 + 按创建时间降序
	    query.addCriteria(Criteria.where("isDelete").is(false));
	    query.with(Sort.by(Sort.Direction.DESC, "createTime"));

	    // 3. 权限过滤
	    switch (personnelType) {
	        case CONSTRUCTION_TEAM: // 维修人员
//	            if (Common.isEmpty(status)) {
//	                query.addCriteria(Criteria.where("status").is(2)); // 默认查已分配
//	            }
	            query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(wxBinding.getId())));
	            break;
	        case DISPATCHER: // 调度人员
	            if (Common.isNotEmpty(workerId)) {
	                query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
	            }
//	            if (Common.isEmpty(status) && Common.isEmpty(search) && Common.isEmpty(workerId)) {
//	                query.addCriteria(Criteria.where("status").is(1)); // 默认查待处理
//	            }
	            break;
	        default:
	            throw new IllegalArgumentException("不支持的人员类型");
	    }

	    // 4. 分页查询（核心）
	    Pagination<WxRepair> pagination = this.findPaginationByQuery(
	        query, pageNo, pageSize, WxRepair.class
	    );
	    return pagination == null ? new Pagination<>() : pagination;
	}

	@Override
	public boolean completeRepair(WxRepair wxRepair, MultipartFile[] repairPhotos, String imgPath, String dir) {
		List<MultiMedia> uploadPictures = this.multiMediaSerice.uploadPictures(repairPhotos, dir, imgPath,
				"WXREPAIRCOMPLETE");
		if (uploadPictures.size() <= 0) {
			return false;
		}

		if (uploadPictures.size() > 0) {
			wxRepair.setRepairPhotos(uploadPictures);
		}
		wxRepair.setStatus(RepairStatus.COMPLETED.getCode());// 订单完成
		try {
			wxRepair.setCompleteTime(Common.getDateYMDHM(new Date()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//维修完成时间

		if (Common.isNotEmpty(wxRepair.getId())) {
			// update
			WxRepair ed = this.findOneById(wxRepair.getId(), WxRepair.class);
			BeanUtils.copyProperties(wxRepair, ed);
			this.save(wxRepair);
			return true;
		}

		return false;
	}

	@Override
	public List<WxRepair> findExportData(String startTime, String endTime, String workerId, Integer status,
			String search) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));

		// 1. 报修日期范围查询
		if (Common.isNotEmpty(startTime) && Common.isNotEmpty(endTime)) {
			try {
				Date start = new SimpleDateFormat("yyyy-MM-dd").parse(startTime);
				Date end = new SimpleDateFormat("yyyy-MM-dd").parse(endTime);
				// 结束日期加一天，包含当天所有数据
				Calendar cal = Calendar.getInstance();
				cal.setTime(end);
				cal.add(Calendar.DATE, 1);
				end = cal.getTime();

				query.addCriteria(Criteria.where("createTime").gte(start).lt(end));
			} catch (ParseException e) {
				log.error("日期格式解析失败", e);
			}
		}

		// 2. 施工队人员筛选
		if (Common.isNotEmpty(workerId)) {
			query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
		}

		// 3. 维修状态筛选
		if (status != null) {
			query.addCriteria(Criteria.where("status").is(status));
		}
		Criteria ca = new Criteria();
		// 4. 学校名称模糊查询
		if (Common.isNotEmpty(search)) {
			List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);

			query.addCriteria(ca.orOperator(Criteria.where("workOrderNumber").regex(search, "i"),
					Criteria.where("faultInformation").regex(search, "i"),
					Criteria.where("urgencyLevel").regex(search, "i"),
					Criteria.where("reportClassroomRepair").regex(search, "i"),
					Criteria.where("equipmentRepair").regex(search, "i"),
					Criteria.where("wxReporter.$id").in(findIdsBySearch)));
		}

		// 排序
		query.with(Sort.by(Sort.Direction.DESC, "createTime"));

		return this.find(query, WxRepair.class);
	}

	@Override
	public WxRepair cancelAssign(String repairId) {
		// 1. 查询报修单
		WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
		if (wxRepair == null) {
			return null;
		}

		// 2. 检查当前状态是否为已分配（状态2）
		if (wxRepair.getStatus() != RepairStatus.ASSIGNED.getCode()) {
			throw new RuntimeException("仅已分配状态的报修单可取消分配");
		}

		// 3. 清空维修人员，状态改为待处理（状态1）
		wxRepair.setWorker(null);
		wxRepair.setStatus(RepairStatus.PENDING.getCode()); 

		// 4. 保存修改
		this.save(wxRepair);

		return wxRepair;
	}

	@Override
	public WxRepair cancelRepair(String repairId) {
		WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
		if (wxRepair == null) {
			return null;
		}
		if (wxRepair.getStatus() == RepairStatus.COMPLETED.getCode()) {
			throw new RuntimeException("订单已完成维修，无法取消！");
		}
		wxRepair.setWorker(null);
		wxRepair.setStatus(RepairStatus.CANCELLED.getCode());
		// 4. 保存修改
		this.save(wxRepair);
		return wxRepair;
	}

}