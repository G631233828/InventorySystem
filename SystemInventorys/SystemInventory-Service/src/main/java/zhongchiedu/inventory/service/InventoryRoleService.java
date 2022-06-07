package zhongchiedu.inventory.service;

import java.util.List;
import java.util.Set;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.InventoryRole;

public interface InventoryRoleService {
	
	
	public Pagination<InventoryRole> findpagination(Integer pageNo,Integer pageSize,String search);
	
	public void saveOrUpdate(InventoryRole inventoryRole,String userIds);
	
	
	public List<InventoryRole> findAllInventoryRole(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name);
	
	public BasicDataResult todisable(String id);
	
	public InventoryRole findByName(String name);
	
	public InventoryRole findByType(String tpye);
	
	public Set<User> findAllUserInInventoryRole();
	

}
