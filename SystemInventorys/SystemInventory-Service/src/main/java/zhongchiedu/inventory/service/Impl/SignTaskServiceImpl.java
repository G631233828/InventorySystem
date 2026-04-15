package zhongchiedu.inventory.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.SignTask;
import zhongchiedu.inventory.service.SignTaskService;
import zhongchiedu.log.annotation.SystemServiceLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SignTaskServiceImpl extends GeneralServiceImpl<SignTask> implements SignTaskService {

    @Override
    public Pagination<SignTask> findpagination(Integer pageNo, Integer pageSize, String search) {
        int pn = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        Pagination<SignTask> page = new Pagination<>();
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("isDelete").is(false));

            if (Common.isNotEmpty(search)) {
                Pattern pattern = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE);
                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("taskName").regex(pattern)
                ));
            }

            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            page = this.findPaginationByQuery(query, pn, ps, SignTask.class);

        } catch (Exception e) {
            page.setPageNo(pn);
            page.setPageSize(ps);
            page.setTotalCount(0);
        }
        return page;
    }

    @Override
    @SystemServiceLog(description = "保存/更新签到任务")
    public void saveOrUpdate(SignTask task) {
        if (Common.isNotEmpty(task.getId())) {
            this.save(task);
        } else {
            this.insert(task);
        }
    }
   
    @Override
    public List<SignTask> findListByQuery(Query query, Class<SignTask> clazz) {
        try {
            Date now = new Date();

            // 你项目里的时间格式是：yyyy-MM-dd HH:mm
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            String nowStr = sdf.format(now);

            // 拼接条件：
            // startTime <= 当前时间
            query.addCriteria(Criteria.where("startTime").lte(nowStr));
            // endTime   >= 当前时间
            query.addCriteria(Criteria.where("endTime").gte(nowStr));
            // 未删除
            query.addCriteria(Criteria.where("isDelete").is(false));
            query.addCriteria(Criteria.where("isDisable").is(false));

            return this.find(query, clazz);

        } catch (Exception e) {
            log.error("根据当前时间查询有效签到任务异常", e);
            return new ArrayList<>();
        }
    }
}