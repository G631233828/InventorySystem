package zhongchiedu.inventory.service.Impl;

import java.util.ArrayList;
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

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.PickUpApplication;
import zhongchiedu.inventory.service.PickUpApplicationService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
public class PickUpApplicationServiceImpl extends GeneralServiceImpl<PickUpApplication>
		implements PickUpApplicationService {

	@Override
	@SystemServiceLog(description = "获取所有待出库信息")
	public Pagination<PickUpApplication> findpagination(Integer pageNo, Integer pageSize, String search,
			String searchArea, String status) {
		// 分页查询数据
		Pagination<PickUpApplication> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(searchArea)) {
				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			}
			if(Common.isEmpty(status)) {
				List<Integer> l = new ArrayList();
				l.add(1);
				l.add(3);
				query.addCriteria(Criteria.where("status").in(l));
			}else {
				query.addCriteria(Criteria.where("status").is(Integer.valueOf(status)));
			}
			
			
		
			
			

			query.addCriteria(Criteria.where("isDelete").is(false));
			 query.with(new Sort(new Order(Direction.DESC, "createTime")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, PickUpApplication.class);
			if (pagination == null)
				pagination = new Pagination<PickUpApplication>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;

	}


	@Override
	@SystemServiceLog(description = "编辑待出库信息")
	public void saveOrUpdate(PickUpApplication stock) {
		if (Common.isNotEmpty(stock)) {
			if (Common.isNotEmpty(stock.getId())) {
				// update
				PickUpApplication ed = this.findOneById(stock.getId(), PickUpApplication.class);
				BeanUtils.copyProperties(stock, ed);
				this.save(stock);
			} else {
				// insert
				this.insert(stock);
			}
		}
	}

	@Override
	@SystemServiceLog(description = "启用禁用待库存信息")
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		PickUpApplication stock = this.findOneById(id, PickUpApplication.class);
		if (Common.isEmpty(stock)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);
		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "恢复成功", stock.getIsDisable());
	}

	@Override
	@SystemServiceLog(description = "获取所有非禁用库存信息")
	public List<PickUpApplication> findAllPickUpApplication(boolean isdisable, String areaId) {
		Query query = new Query();
		if (Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, PickUpApplication.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除待出库信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				PickUpApplication de = this.findOneById(edid, PickUpApplication.class);
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
	@SystemServiceLog(description = "启用禁用待出库信息")
	public BasicDataResult todisable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		PickUpApplication stock = this.findOneById(id, PickUpApplication.class);
		if (stock == null) {
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);

		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "启用成功", stock.getIsDisable());
	}


	@Override
	public List<PickUpApplication> findAllPickUpApplicationByStatus(boolean isdisable, int status) {
		Query query = new Query();
		if (Common.isNotEmpty(status)) {
			query.addCriteria(Criteria.where("status").is(status));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, PickUpApplication.class);
	}


	@Override
	public List<PickUpApplication> findPickUpApplicationsByStockId(String stockId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(stockId)))
				.addCriteria(Criteria.where("isDelete").is(false))
				.addCriteria(Criteria.where("isDisable").is(false))
				.addCriteria(Criteria.where("status").ne(2));
		return this.find(query, PickUpApplication.class);
		
	}

}
