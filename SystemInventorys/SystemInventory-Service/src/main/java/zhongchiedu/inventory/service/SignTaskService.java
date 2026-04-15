package zhongchiedu.inventory.service;

import java.util.List;

import org.springframework.data.mongodb.core.query.Query;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.SignTask;

public interface SignTaskService extends GeneralService<SignTask> {
    Pagination<SignTask> findpagination(Integer pageNo, Integer pageSize, String search);
    void saveOrUpdate(SignTask task);
	List<SignTask> findListByQuery(Query query, Class<SignTask> class1);
}