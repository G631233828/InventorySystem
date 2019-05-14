package zhongchiedu.inventory.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.Category;

public interface CategoryService extends GeneralService<Category> {
	
	public Pagination<Category> findpagination(Integer pageNo,Integer pageSize);
	
	public void saveOrUpdate(Category category);
	
	public BasicDataResult disable(String id);
	
	public List<Category> findAllCategory(boolean isdisable);
	
	public String delete(String id);
	
	public BasicDataResult ajaxgetRepletes(String name);
	
	public BasicDataResult todisable(String id);
	

}
