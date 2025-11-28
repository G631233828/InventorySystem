package zhongchiedu.inventory.service.Impl;

import java.util.Arrays;
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
	public Pagination<WxRepair> findpagination(Integer pageNo, Integer pageSize) {
		// 分页查询数据
		Pagination<WxRepair> pagination = null;
		try {
			Query query = new Query();

			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, WxRepair.class);
			if (pagination == null)
				pagination = new Pagination<WxRepair>();
			return pagination;
		} catch (Exception e) {
			log.info("查询所有报修信息失败——————————》" + e.toString());
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	public void saveOrUpdate(WxRepair wxRepair, MultipartFile[] photos, String imgPath, String dir) {

		List<MultiMedia> uploadPictures = this.multiMediaSerice.uploadPictures(photos, dir, imgPath, "WXREPAIR");
		if (uploadPictures.size() > 0) {
			wxRepair.setPhotos(uploadPictures);
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
	 * 维修人员 调度人员查看所有报修信息
	 */
	@Override
	public List<WxRepair> findOperationsWxRepairByOpenId(String openId,String search,Integer status,String workerId) {
		// 通过openId 去查看opendId所属人员权限
		WxBinding wxBinding = this.wxBindingService.findWxBindingByOpenId(openId);
		PersonnelType personnelType = PersonnelType.getByCode(wxBinding.getPersonnelType())
				.orElseThrow(() -> new IllegalArgumentException("无效的人员类型编码：" + wxBinding.getPersonnelType()));

		Query query = new Query();
		Criteria ca = new Criteria();
		if(Common.isNotEmpty(search)) {
			//根据search的内容去用户绑定里面查询id
			List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);
			
		
			query.addCriteria(ca.orOperator(Criteria.where("workOrderNumber").regex(search),
					Criteria.where("faultInformation").regex(search),
					Criteria.where("urgencyLevel").regex(search),
					Criteria.where("reportClassroomRepair").regex(search),
					Criteria.where("equipmentRepair").regex(search),Criteria.where("wxReporter.$id").in(findIdsBySearch)));
			
//			if(findIdsBySearch.size()>0) {
//				ca.orOperator(Criteria.where("wxReporter.$id").in(findIdsBySearch));
//			}
			
		}
		if(Common.isNotEmpty(status)) {
			query.addCriteria(Criteria.where("status").is(status));
		}
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		switch (personnelType) {
		case CONSTRUCTION_TEAM:
			if(Common.isEmpty(status)) {
				//施工队默认查看状态为2的数据
				query.addCriteria(Criteria.where("status").is(2));
			}
			// 维修人员 只能根据自己的openId查看自己的维修数据
			query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(wxBinding.getId())));
			break;
		case DISPATCHER:
			if(Common.isNotEmpty(workerId)) {
				query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
			}
			if(Common.isEmpty(status)&&Common.isEmpty(search)&&Common.isEmpty(workerId)) {
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
		wxRepair.setStatus(RepairStatus.COMPLETED.getCode());//订单完成
		
		if (Common.isNotEmpty(wxRepair.getId())) {
			// update
			
			WxRepair ed = this.findOneById(wxRepair.getId(), WxRepair.class);
			BeanUtils.copyProperties(wxRepair, ed);
			this.save(wxRepair);
			return true;
		}

		return false;
	}


}
