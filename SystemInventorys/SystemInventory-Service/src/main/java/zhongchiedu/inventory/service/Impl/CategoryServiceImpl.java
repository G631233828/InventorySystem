package zhongchiedu.inventory.service.Impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.service.CategoryService;

@Service
@Slf4j
public class CategoryServiceImpl extends GeneralServiceImpl<Category> implements CategoryService {

	@Override
	public void saveOrUpdate(Category category) {
		if (Common.isNotEmpty(category)) {
			//检查是否存在
			BasicDataResult rs = ajaxgetRepletes(category.getName());
			if(rs.getStatus() == 206){
				category.setName(category.getName()+"(2)");
				log.info("添加的数据已经存在，正在改名");
			}
			if (Common.isNotEmpty(category.getId())) {
				// update
				Category ed = this.findOneById(category.getId(), Category.class);
				BeanUtils.copyProperties(category, ed);
				this.save(category);
				log.info("修改成功");
			} else {
				// insert
				this.insert(category);
				log.info("添加成功");
			}
		}
	}

	@Override
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Category category = this.findOneById(id, Category.class);
		if (Common.isEmpty(category)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		category.setIsDisable(category.getIsDisable().equals(true) ? false : true);
		this.save(category);
		return BasicDataResult.build(200, category.getIsDisable().equals(true) ? "禁用成功" : "恢复成功",
				category.getIsDisable());
	}

	@Override
	public List<Category> findAllCategory(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Category.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Category de = this.findOneById(edid, Category.class);
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
	public Pagination<Category> findpagination(Integer pageNo, Integer pageSize) {
		// 分页查询数据
		Pagination<Category> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Category.class);
			if (pagination == null)
				pagination = new Pagination<Category>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	public BasicDataResult ajaxgetRepletes(String name) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			Category category = this.findOneByQuery(query, Category.class);
			 return category != null ?BasicDataResult.build(206,"当前货架信息已经存在，请检查", null): BasicDataResult.ok();
		}
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
	}

	@Override
	public BasicDataResult todisable(String id) {
		
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Category category = this.findOneById(id, Category.class);
		if(category == null){
			return BasicDataResult.build(400, "无法获取到企业信息，该用户可能已经被删除", null);
		}
		category.setIsDisable(category.getIsDisable().equals(true)?false:true);
		this.save(category);
		
		return BasicDataResult.build(200, category.getIsDisable().equals(true)?"禁用成功":"启用成功",category.getIsDisable());
		
	}



}
