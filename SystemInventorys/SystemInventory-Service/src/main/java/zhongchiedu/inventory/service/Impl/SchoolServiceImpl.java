package zhongchiedu.inventory.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.School;
import zhongchiedu.inventory.service.SchoolService;
import zhongchiedu.log.annotation.SystemServiceLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SchoolServiceImpl extends GeneralServiceImpl<School> implements SchoolService {

    private final Lock lock = new ReentrantLock();

    @Override
    @SystemServiceLog(description = "分页查询学校信息")
    public Pagination<School> findpagination(Integer pageNo, Integer pageSize, String search) {
        int finalPageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int finalPageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        Pagination<School> pagination = new Pagination<>();
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("isDelete").is(false));

            if (Common.isNotEmpty(search)) {
                String regex = Pattern.quote(search.trim());
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                List<Criteria> searchCriterias = new ArrayList<>();

                searchCriterias.add(Criteria.where("schoolName").regex(pattern));
                searchCriterias.add(Criteria.where("address").regex(pattern));
                searchCriterias.add(Criteria.where("contact").regex(pattern));
                searchCriterias.add(Criteria.where("phone").regex(pattern));
                searchCriterias.add(Criteria.where("area").regex(pattern));
                searchCriterias.add(Criteria.where("schoolType").regex(pattern));

                query.addCriteria(new Criteria().orOperator(searchCriterias.toArray(new Criteria[0])));
            }

            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            pagination = this.findPaginationByQuery(query, finalPageNo, finalPageSize, School.class);

        } catch (Exception e) {
            log.error("分页查询学校失败", e);
            pagination.setPageNo(finalPageNo);
            pagination.setPageSize(finalPageSize);
            pagination.setTotalCount(0);
        }
        return pagination;
    }

    @Override
    @SystemServiceLog(description = "编辑学校信息")
    public void saveOrUpdate(School school) {
        if (Common.isNotEmpty(school)) {
            if (Common.isNotEmpty(school.getId())) {
                School old = this.findOneById(school.getId(), School.class);
                BeanUtils.copyProperties(school, old);
                this.save(school);
            } else {
                this.insert(school);
            }
        }
    }

    @Override
    @SystemServiceLog(description = "查询所有学校信息")
    public List<School> findAllName(boolean isdisable) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("isDelete").is(false));
            query.addCriteria(Criteria.where("isDisable").is(isdisable));
            query.with(Sort.by(Sort.Direction.ASC, "schoolName"));
            return this.find(query, School.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ===================== 批量导入学校（完全参照你的写法）=====================
    @Override
    @SystemServiceLog(description = "批量导入学校信息")
    public String BatchImport(File file, int row, HttpSession session) {
        StringBuilder errorMsg = new StringBuilder();
        String[][] excelData;

        try {
            excelData = ExcelReadUtil.readExcel(file, 0);
        } catch (IOException e) {
            return "Excel读取失败：" + e.getMessage();
        }

        if (excelData == null || excelData.length <= 1) {
            return "Excel无有效数据";
        }

        ProcessInfo processInfo = new ProcessInfo();
        processInfo.allnum = excelData.length - 1;
        session.setAttribute("proInfo", processInfo);

        int success = 0, fail = 0;

        for (int i = 1; i < excelData.length; i++) {
            int line = i + 1;
            try {
                processInfo.nownum = i;
                session.setAttribute("proInfo", processInfo);

                String[] data = excelData[i];
                if (data == null) continue;

                // 学校名称（必填）
                String schoolName = data.length > 0 ? data[0].trim() : "";
                if (Common.isEmpty(schoolName)) {
                    fail++;
                    errorMsg.append("第").append(line).append("行：学校名称不能为空<br>");
                    continue;
                }

                // 重复校验
                if (isExist(schoolName)) {
                    fail++;
                    errorMsg.append("第").append(line).append("行：学校【").append(schoolName).append("】已存在<br>");
                    continue;
                }

                School school = new School();
                school.setSchoolName(schoolName);
                school.setAddress(data.length > 1 ? data[1].trim() : "");
                school.setIsDelete(false);
                school.setIsDisable(false);
                school.setCreateTime(new Date());

                this.insert(school);
                success++;

            } catch (Exception e) {
                fail++;
                errorMsg.append("第").append(line).append("行：导入失败<br>");
            }
        }

        session.removeAttribute("proInfo");
        return errorMsg.length() > 0 ? errorMsg.toString() : "成功导入 " + success + " 条数据";
    }

    private boolean isExist(String schoolName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("isDelete").is(false));
        query.addCriteria(Criteria.where("schoolName").is(schoolName));
        return this.find(query, School.class).size() > 0;
    }

    @Override
    public String upload(HttpServletRequest request, HttpSession session) {
        try {
            String path = File.separator + "FileUpload" + File.separator + "school";
            String[] types = {"xls", "xlsx"};
            List<Map<String, Object>> result = FileOperateUtil.upload(request, path, types);

            if (result == null || result.isEmpty()) return "未获取到文件";
            boolean ok = (boolean) result.get(0).get("hassuffix");
            if (!ok) return "仅支持xls/xlsx";

            String filePath = (String) result.get(0).get("savepath");
            return BatchImport(new File(filePath), 1, session);
        } catch (Exception e) {
            return "上传失败：" + e.getMessage();
        }
    }

    @Override
    public ProcessInfo findproInfo(HttpServletRequest request) {
        return (ProcessInfo) request.getSession().getAttribute("proInfo");
    }

    @Override
    public List<ObjectId> findIdsBySearch(String search) {
        if (Common.isEmpty(search)) return null;
        Query query = new Query();
        query.addCriteria(Criteria.where("schoolName").regex(search));
        List<School> list = this.find(query, School.class);
        return list.stream().map(s -> new ObjectId(s.getId())).collect(Collectors.toList());
    }
}