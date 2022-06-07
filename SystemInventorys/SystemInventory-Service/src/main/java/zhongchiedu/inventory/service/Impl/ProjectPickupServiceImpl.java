package zhongchiedu.inventory.service.Impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import zhongchiedu.inventory.pojo.ProjectPickup;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.service.ProjectPickupService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
public class ProjectPickupServiceImpl extends GeneralServiceImpl<ProjectPickup> implements ProjectPickupService {

	@Override
	public void saveOrUpdate(ProjectPickup projectPickup) {
		if (projectPickup!=null) {
			if (Common.isNotEmpty(projectPickup.getId())) {
				// 修改取货信息
				ProjectPickup ed = this.findOneById(projectPickup.getId(), ProjectPickup.class);
				BeanUtils.copyProperties(projectPickup, ed);
				this.save(projectPickup);
			} else {
				// 新增取货
				this.insert(projectPickup);
			}
		}
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除取货信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				ProjectPickup de = this.findOneById(edid, ProjectPickup.class);
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
	public BasicDataResult todisable(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pagination<ProjectPickup> findpagination(Integer pageNo, Integer pageSize, String search, String start,
			String end) {
		Pagination<ProjectPickup> pagination = null;
		try {
			Query query = new Query();
			Criteria ca = new Criteria();
			Criteria ca2 = new Criteria();
			if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
				ca2.andOperator(Criteria.where("createTime").gte(Common.getDateByStringDate(start)),
						Criteria.where("createTime").lte(Common.getDateByStringDate(end)));
			}

//			if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
//			query.addCriteria(Criteria.where("createTime").gte(Common.getDateByStringDate(start)));
//			}
			if(Common.isNotEmpty(search)) {
				ca.orOperator(Criteria.where("name").regex(search), Criteria.where("model").regex(search),
						Criteria.where("phone").regex(search), Criteria.where("username").regex(search),
						Criteria.where("entryName").regex(search),Criteria.where("itemNo").regex(search),
						Criteria.where("projectLeader").regex(search));
			}
			query.addCriteria(ca.andOperator(ca2));
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, ProjectPickup.class);
			if (pagination == null)
				pagination = new Pagination<ProjectPickup>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	public Map<String, Object> getExportData(String id, String search, String start, String end) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Map<String, Object> dataMap = new HashMap<String, Object>();
		if (Common.isEmpty(id)) {
			// 搜索条件导出
			Query query = new Query();
			Criteria ca = new Criteria();
			Criteria ca2 = new Criteria();
			if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
				ca2.andOperator(Criteria.where("createTime").gte(Common.getDateByStringDate(start)),
						Criteria.where("createTime").lte(Common.getDateByStringDate(end)));
			}
			if(Common.isNotEmpty(search)) {
				ca.orOperator(Criteria.where("name").regex(search), Criteria.where("model").regex(search),
						Criteria.where("phone").regex(search), Criteria.where("username").regex(search),
						Criteria.where("entryName").regex(search),Criteria.where("itemNo").regex(search),
						Criteria.where("projectLeader").regex(search));
			}
			query.addCriteria(ca.andOperator(ca2));
			query.addCriteria(Criteria.where("isDelete").is(false));
			ProjectPickup projectPickup = this.findOneByQuery(query, ProjectPickup.class);
			Map<String, String> data = this.findMapData(projectPickup);
			list.add(data);

		} else {
			List<String> ids = Arrays.asList(id.split(","));
			ids.forEach(a -> {
				ProjectPickup projectPickup = this.findOneById(a, ProjectPickup.class);
				Map<String, String> data = this.findMapData(projectPickup);
				list.add(data);
			});
		}

		dataMap.put("list", list);
		return dataMap;
	}

	
	/**
	 * 处理数据
	 * @param projectPickup
	 * @return
	 */
	public Map<String,String> findMapData(ProjectPickup projectPickup){
		if (projectPickup!=null) {
			Map<String, String> data = new HashMap<String, String>();
			data.put("name", projectPickup.getName());
			data.put("model", projectPickup.getModel());
			data.put("entryName", projectPickup.getEntryName());
			data.put("itemNo", projectPickup.getItemNo());
			data.put("projectLeader", projectPickup.getProjectLeader());
			data.put("username", projectPickup.getUsername());
			data.put("num", Common.isEmpty(projectPickup.getNum())?"":projectPickup.getNum().toString());
			data.put("phone", projectPickup.getPhone());
			try {
				data.put("time", Common.getDateYMDHM(projectPickup.getCreateTime()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			data.put("sign", projectPickup.getSign().replaceAll("data:image/png;base64,", ""));
			return data;
		}
		return null;
	}
}
