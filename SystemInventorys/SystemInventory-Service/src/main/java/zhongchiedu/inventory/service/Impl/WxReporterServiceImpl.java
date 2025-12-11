package zhongchiedu.inventory.service.Impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.Impl.MultiMediaServiceImpl;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.inventory.service.WxReporterService;

@Service
@Slf4j
public class WxReporterServiceImpl extends GeneralServiceImpl<WxReporter> implements WxReporterService{
	

	
	
	
	@Override
	public Pagination<WxReporter> findpagination(Integer pageNo, Integer pageSize) {
		// 分页查询数据
				Pagination<WxReporter> pagination = null;
				try {
					Query query = new Query();

					query.addCriteria(Criteria.where("isDelete").is(false));
					query.with(new Sort(new Order(Direction.DESC, "createTime")));
					pagination = this.findPaginationByQuery(query, pageNo, pageSize, WxReporter.class);
					if (pagination == null)
						pagination = new Pagination<WxReporter>();
					return pagination;
				} catch (Exception e) {
					log.info("查询所有报修人员失败——————————》" + e.toString());
					e.printStackTrace();
				}
				return pagination;
	}

	@Override
	public WxReporter saveOrUpdate(WxReporter wxReporter) {
		
		if (Common.isNotEmpty(wxReporter.getOpenId())) {
			//根据openId获取报修人信息
		Query query = new Query();
		query.addCriteria(Criteria.where("openId").is(wxReporter.getOpenId()));
		WxReporter getwxReporter = this.findOneByQuery(query, WxReporter.class);	
		  if (getwxReporter == null) {
		        log.info("未找到 openId 为 [{}] 的报修人，执行新增操作。", wxReporter.getOpenId());
		        this.insert(wxReporter);
		        return wxReporter;
		    }
		  // 4. 处理已存在的情况（判断是否需要更新）
		    boolean isInfoChanged = false;
		    // 逐一比较关键信息字段
		    if (!Objects.equals(getwxReporter.getSchoolAddress(), wxReporter.getSchoolAddress())) {
		        isInfoChanged = true;
		    } else if (!Objects.equals(getwxReporter.getCampus(), wxReporter.getCampus())) {
		        isInfoChanged = true;
		    } else if (!Objects.equals(getwxReporter.getUserName(), wxReporter.getUserName())) {
		        isInfoChanged = true;
		    } else if (!Objects.equals(getwxReporter.getContactNumber(), wxReporter.getContactNumber())) {
		        isInfoChanged = true;
		    } else if (!Objects.equals(getwxReporter.getSchoolName(), wxReporter.getSchoolName())) {
		        isInfoChanged = true;
		    }
		    // 5. 根据判断结果执行更新或跳过
		    if (isInfoChanged) {
		        log.info("报修人 [{}] 的信息发生变化，执行更新操作。", wxReporter.getOpenId());
		        // 将 existingReporter 的 ID 赋值给新对象，以确保是更新操作而不是新增
		        wxReporter.setId(getwxReporter.getId()); 
		        this.save(wxReporter); // 这里的 save 方法应该是既能新增也能更新的
		        return wxReporter;
		    } else {
		        log.info("报修人 [{}] 的信息未发生变化，跳过更新操作。", wxReporter.getOpenId());
		        return getwxReporter;
		    }
			
			
			
		}
		return wxReporter;
	}
	
	private Lock lock = new ReentrantLock();
	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				WxReporter de = this.findOneById(edid, WxReporter.class);
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
	public WxReporter findWxReporterByOpenId(String openId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		query.addCriteria(Criteria.where("openId").is(openId));
		return this.findOneByQuery(query, WxReporter.class);
	}
	

	@Override
	public List<ObjectId> findIdsBySearch(String search) {
		
		if(Common.isNotEmpty(search)) {
			Query query = new Query();
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(Criteria.where("schoolName").regex(search),Criteria.where("schoolAddress").regex(search),Criteria.where("campus").regex(search),Criteria.where("userName").regex(search)));
			List<WxReporter> wxreporters = this.find(query, WxReporter.class);
			return wxreporters.stream()
		            .map(wxReporter -> {
		                String id = wxReporter.getId();
		                    return new ObjectId(id);
		            })
		            .filter(Objects::nonNull)
		            .collect(Collectors.toList());
		}
		
		
		return null;
	}

}
