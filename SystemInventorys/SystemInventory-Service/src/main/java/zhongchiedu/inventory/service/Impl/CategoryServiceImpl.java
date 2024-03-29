package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.service.CategoryService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class CategoryServiceImpl extends GeneralServiceImpl<Category> implements CategoryService {

	@Override
	@SystemServiceLog(description="编辑类目信息")
	public void saveOrUpdate(Category category) {
		if (Common.isNotEmpty(category)) {
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
	@SystemServiceLog(description="查询所有非禁用的类目")
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
	@SystemServiceLog(description="分页查询类目信息")
	public Pagination<Category> findpagination(Integer pageNo, Integer pageSize,String search) {
		// 分页查询数据
		Pagination<Category> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			if(Common.isNotEmpty(search)) {
				query.addCriteria(Criteria.where("name").regex(search));
			}
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
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
	@SystemServiceLog(description="根据类目名称查询重复")
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
	@SystemServiceLog(description="启用禁用类目")
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
	
	
	@SystemServiceLog(description="批量导入类目")
	public String  BatchImport(File file, int row, HttpSession session) {
		String error = "";
		String[][] resultexcel = null;
		try {
			resultexcel = ExcelReadUtil.readExcel(file, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int rowLength = resultexcel.length;
		ProcessInfo pri = new ProcessInfo();
		pri.allnum = rowLength;
		for (int i = 1; i < rowLength; i++) {
			Query query = new Query();
			Category importCategory = new Category();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {
				importCategory.setName(resultexcel[i][j]);
				
				query.addCriteria(Criteria.where("name").is(importCategory.getName()));
				query.addCriteria(Criteria.where("isDelete").is(false));
				// 通过类目名称是否存在该信息
				Category category = this.findOneByQuery(query, Category.class);
				if(Common.isNotEmpty(category)){
					error += "<span class='entypo-attention'></span>导入文件过程中出现已经存在的类目信息，第<b>&nbsp&nbsp" + (i + 1)
							+ "&nbsp&nbsp</b>行出现重复内容为<b>&nbsp&nbsp导入类目名称为:" + category.getName()
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}else{
					this.insert(importCategory);
				}
				// 捕捉批量导入过程中遇到的错误，记录错误行数继续执行下去
			} catch (Exception e) {
				log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" + e);
				String aa = e.getLocalizedMessage();
				String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
				error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1)
						+ "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br>";
				if ((i + 1) < rowLength) {
					continue;
				}

			}
		}
		log.info(error);
		return error;
	}

	
	
/**
 * 执行上传文件，返回错误消息	
 */
	@SystemServiceLog(description="上传类目信息")
public String upload( HttpServletRequest request, HttpSession session){
	String error = "";
	try {
		Map<String, Object> map = new HashMap<String, Object>();
		// 别名
		String upname = File.separator + "FileUpload" + File.separator + "category";

		// 可以上传的文件格式
		log.info("准备上传类目数据");
		String filetype[] = { "xls,xlsx" };
		List<Map<String, Object>> result = FileOperateUtil.upload(request, upname, filetype);
		log.info("上传文件成功");
		boolean has = (Boolean) result.get(0).get("hassuffix");

		if (has != false) {
			// 获得上传的xls文件路径
			String path = (String) result.get(0).get("savepath");
			File file = new File(path);
			// 知道导入返回导入结果
			error = this.BatchImport(file, 1,session);
		}
	} catch (Exception e) {
		return e.toString();
	}
	return error;
	
}
	
	
	/**
	 * 上传进度
	 */
	@Override
	public ProcessInfo findproInfo(HttpServletRequest request) {

		return (ProcessInfo) request.getSession().getAttribute("proInfo");

	}

	
	/**
	 * 根据类目名车查找类目，如果没有则创建一个
	 */
	@Override
	@SystemServiceLog(description="根据类目名称查询类目信息")
	public Category findByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("isDelete").is(false));
		Category category = this.findOneByQuery(query, Category.class);
		if(Common.isEmpty(category)){
			Category ca = new Category();
			ca.setName(name);
			this.insert(ca);
			return ca;
		}
		return category;
	}
	

}
