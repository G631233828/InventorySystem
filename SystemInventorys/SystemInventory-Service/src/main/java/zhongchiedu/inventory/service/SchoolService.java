package zhongchiedu.inventory.service;

import org.bson.types.ObjectId;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.School;
import zhongchiedu.inventory.pojo.ProcessInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.List;

public interface SchoolService extends GeneralService<School> {

    Pagination<School> findpagination(Integer pageNo, Integer pageSize, String search);

    void saveOrUpdate(School school);

    List<School> findAllName(boolean isdisable);

    String BatchImport(File file, int row, HttpSession session);

    String upload(HttpServletRequest request, HttpSession session);

    ProcessInfo findproInfo(HttpServletRequest request);

    List<ObjectId> findIdsBySearch(String search);
}