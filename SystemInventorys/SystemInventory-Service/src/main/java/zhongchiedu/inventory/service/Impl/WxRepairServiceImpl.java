package zhongchiedu.inventory.service.Impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.service.WxRepairService;

@Service
@Slf4j
public class WxRepairServiceImpl extends GeneralServiceImpl<WxRepair> implements WxRepairService{
	
	
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
	public void saveOrUpdate(WxRepair wxRepair) {
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


}
