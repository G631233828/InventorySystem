package zhongchiedu.inventory.service.Impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class AreaServiceImpl extends GeneralServiceImpl<Area> implements AreaService {

	@Override
	@SystemServiceLog(description="编辑区域信息")
	public void saveOrUpdate(Area area) {
		if (Common.isNotEmpty(area)) {
			if (Common.isNotEmpty(area.getId())) {
				// update
				Area ed = this.findOneById(area.getId(), Area.class);
				BeanUtils.copyProperties(area, ed);
				this.save(area);
				log.info("修改成功");
			} else {
				// insert
				this.insert(area);
				log.info("添加成功");
			}
		}
	}


	@Override
	@SystemServiceLog(description="查询所有区域信息")
	public List<Area> findAllArea(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Area.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description="删除区域信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Area de = this.findOneById(edid, Area.class);
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
	@SystemServiceLog(description="查询区域信息")
	public Pagination<Area> findpagination(Integer pageNo, Integer pageSize,String search) {
		// 分页查询数据
		Pagination<Area> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			if(Common.isNotEmpty(search)) {
				query.addCriteria(Criteria.where("name").regex(search));
			}
			
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Area.class);
			if (pagination == null)
				pagination = new Pagination<Area>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	@SystemServiceLog(description="查询重复区域信息")
	public BasicDataResult ajaxgetRepletes(String name) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			Area brand = this.findOneByQuery(query, Area.class);
			 return brand != null ?BasicDataResult.build(206,"当前货架信息已经存在，请检查", null): BasicDataResult.ok();
		}
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
	}

	@Override
	@SystemServiceLog(description="禁用区域信息")
	public BasicDataResult todisable(String id) {
		
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Area brand = this.findOneById(id, Area.class);
		if(brand == null){
			return BasicDataResult.build(400, "无法获取到企业信息，该用户可能已经被删除", null);
		}
		brand.setIsDisable(brand.getIsDisable().equals(true)?false:true);
		this.save(brand);
		
		return BasicDataResult.build(200, brand.getIsDisable().equals(true)?"禁用成功":"启用成功",brand.getIsDisable());
		
	}
	
	
	

	
	
	
	/**
	 * 根据单位名称查找单位，如果没有则创建一个
	 */
	@Override
	@SystemServiceLog(description="根据名称查询区域信息")
	public Area findByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("isDelete").is(false));
		Area brand = this.findOneByQuery(query, Area.class);
		if(Common.isEmpty(brand)){
			Area ca = new Area();
			ca.setName(name);
			this.insert(ca);
			return ca;
		}
		return brand;
	}

	@Override
	public List findIdsByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(name));
		query.addCriteria(Criteria.where("isDelete").is(false));		
		query.addCriteria(Criteria.where("isDisable").is(false));		
		
		List<Area> areas = this.find(query, Area.class);
		
		List list = new ArrayList();
		areas.forEach(area->{
			list.add(new ObjectId(area.getId()));
			
		});
		return list;
	}
	

}
