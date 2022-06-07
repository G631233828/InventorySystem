package zhongchiedu.inventory.service.Impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.InventoryRole;
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class InventoryRoleServiceImpl extends GeneralServiceImpl<InventoryRole> implements InventoryRoleService {
	
	
	@Autowired
	private UserService userService;
	

	@Override
	public Pagination<InventoryRole> findpagination(Integer pageNo, Integer pageSize, String search) {
		// 分页查询数据
		Pagination<InventoryRole> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			if (Common.isNotEmpty(search)) {
				query.addCriteria(Criteria.where("name").regex(search));
			}
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, InventoryRole.class);
			if (pagination == null)
				pagination = new Pagination<InventoryRole>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	public void saveOrUpdate(InventoryRole inventoryRole,String userIds) {
		if (Common.isNotEmpty(inventoryRole)) {
			
			String[] ids = userIds.split(",");
			List<Object> list= new ArrayList();
			Arrays.asList(ids).stream().forEach(id->{
				list.add(new ObjectId(id));
			});
			List<User> users = this.userService.findUserInIds(list);
			inventoryRole.setUsers(users);
			if (Common.isNotEmpty(inventoryRole.getId())) {
				// update
				InventoryRole ed = this.findOneById(inventoryRole.getId(), InventoryRole.class);
				BeanUtils.copyProperties(inventoryRole, ed);
				this.save(inventoryRole);
				log.info("修改成功");
			} else {
				// insert
				this.insert(inventoryRole);
				log.info("添加成功");
			}
		}
	}

	

	@Override
	public List<InventoryRole> findAllInventoryRole(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, InventoryRole.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除角色信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				InventoryRole de = this.findOneById(edid, InventoryRole.class);
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
	public BasicDataResult ajaxgetRepletes(String name) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			InventoryRole o = this.findOneByQuery(query, InventoryRole.class);
			return o != null ? BasicDataResult.build(206, "当前信息已经存在，请检查", null) : BasicDataResult.ok();
		}
		return BasicDataResult.build(400, "未能获取到请求的信息", null);
	}

	@Override
	public BasicDataResult todisable(String id) {

		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		InventoryRole o = this.findOneById(id, InventoryRole.class);
		if (o == null) {
			return BasicDataResult.build(400, "无法获取到企业信息，该用户可能已经被删除", null);
		}
		o.setIsDisable(o.getIsDisable().equals(true) ? false : true);
		this.save(o);
		return BasicDataResult.build(200, o.getIsDisable().equals(true) ? "禁用成功" : "启用成功", o.getIsDisable());
	}

	@Override
	public InventoryRole findByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("isDelete").is(false));
		InventoryRole o = this.findOneByQuery(query, InventoryRole.class);
		return o;
	}

	@Override
	public InventoryRole findByType(String tpye) {
		Query query = new Query();
		query.addCriteria(Criteria.where("type").is(tpye));
		query.addCriteria(Criteria.where("isDelete").is(false));
		InventoryRole o = this.findOneByQuery(query, InventoryRole.class);
		return o;
	}
	
	public Set<User> findAllUserInInventoryRole(){
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<InventoryRole> inventoryRole = this.find(query, InventoryRole.class);
		Set users = new HashSet();
		inventoryRole.forEach(in->{
			in.getUsers().forEach(user->{
					users.add(user);
			});
		});
		return users;
	}
	

}
