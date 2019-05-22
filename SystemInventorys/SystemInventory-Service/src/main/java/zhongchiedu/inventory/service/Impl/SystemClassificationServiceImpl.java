package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.service.SystemClassificationService;

@Service
@Slf4j
public class SystemClassificationServiceImpl extends GeneralServiceImpl<SystemClassification>
		implements SystemClassificationService {

	@Autowired
	private CategoryServiceImpl categoryService;

	@Override
	public void saveOrUpdate(SystemClassification systemClassification, String types) {
		if (Common.isNotEmpty(systemClassification)) {
			if (Common.isNotEmpty(types)) {
				List<String> ids = Arrays.asList(types.split(","));
				Query query = new Query();
				query.addCriteria(Criteria.where("_id").in(ids));
				List<Category> list = this.categoryService.find(query, Category.class);
				systemClassification.setCategorys(list);
			}
			if (Common.isNotEmpty(systemClassification.getId())) {
				// update
				SystemClassification ed = this.findOneById(systemClassification.getId(), SystemClassification.class);
				BeanUtils.copyProperties(systemClassification, ed);
				this.save(systemClassification);
			} else {
				// insert
				this.insert(systemClassification);
			}
		}
	}

	@Override
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		SystemClassification systemClassification = this.findOneById(id, SystemClassification.class);
		if (Common.isEmpty(systemClassification)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		systemClassification.setIsDisable(systemClassification.getIsDisable().equals(true) ? false : true);
		this.save(systemClassification);
		return BasicDataResult.build(200, systemClassification.getIsDisable().equals(true) ? "禁用成功" : "恢复成功",
				systemClassification.getIsDisable());
	}

	@Override
	public List<SystemClassification> findAllSystemClassification(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, SystemClassification.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				SystemClassification de = this.findOneById(edid, SystemClassification.class);
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
	public Pagination<SystemClassification> findpagination(Integer pageNo, Integer pageSize) {
		// 分页查询数据
		Pagination<SystemClassification> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, SystemClassification.class);
			if (pagination == null)
				pagination = new Pagination<SystemClassification>();
			return pagination;
		} catch (Exception e) {
			log.info("查询所有系统分类信息失败——————————》" + e.toString());
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
			SystemClassification category = this.findOneByQuery(query, SystemClassification.class);
			return category != null ? BasicDataResult.build(206, "当前系统分类已经存在，请检查", null) : BasicDataResult.ok();
		}
		return BasicDataResult.build(400, "未能获取到请求的信息", null);
	}

	@Override
	public BasicDataResult todisable(String id) {

		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		SystemClassification systemClassification = this.findOneById(id, SystemClassification.class);
		if (systemClassification == null) {
			return BasicDataResult.build(400, "无法获取到企业信息，该用户可能已经被删除", null);
		}
		systemClassification.setIsDisable(systemClassification.getIsDisable().equals(true) ? false : true);
		this.save(systemClassification);

		return BasicDataResult.build(200, systemClassification.getIsDisable().equals(true) ? "禁用成功" : "启用成功",
				systemClassification.getIsDisable());

	}

	public String BatchImport(File file, int row, HttpSession session) {
		String error = "";
		String[][] resultexcel = null;
		try {
			resultexcel = ExcelReadUtil.readExcel(file, row);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int rowLength = resultexcel.length;
		ProcessInfo pri = new ProcessInfo();
		pri.allnum = rowLength;
		for (int i = 0; i < rowLength; i++) {
			Query query = new Query();
			SystemClassification importSystemClassification = new SystemClassification();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {
				importSystemClassification.setName(resultexcel[i][j]);
				String categoryName = resultexcel[i][j+1];
				Category category = null;
				if(Common.isNotEmpty(categoryName)){
					category = this.categoryService.findByName(categoryName);
				}
				
				// 通过系統分类名称查询是否存在系统分类
				SystemClassification systemClassification = this.findByName(importSystemClassification.getName());
				if(Common.isNotEmpty(systemClassification)){
					//导入类目
					List<Category>  list = systemClassification.getCategorys();
					
					if(list.contains(category)){
						error += "<span class='entypo-attention'></span>导入文件过程中出现已经在相同系统分类下的类目信息，第<b>&nbsp&nbsp" + (i + 2)
								+ "&nbsp&nbsp</b>行出现重复内容为<b>&nbsp&nbsp导入类目名称为:<b>&nbsp&nbsp" + categoryName
								+ "&nbsp&nbsp</b>请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
					list.add(category);
					this.save(systemClassification);
				}else{
					//创建系统分类，导入类目
					List<Category> list = new ArrayList<>();
					list.add(category);
					importSystemClassification.setCategorys(list);
					this.insert(importSystemClassification);
				}
				// 捕捉批量导入过程中遇到的错误，记录错误行数继续执行下去
			} catch (Exception e) {
				log.debug("导入文件过程中出现错误第" + (i + 2) + "行出现错误" + e);
				String aa = e.getLocalizedMessage();
				String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
				error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 2)
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
	public String upload(HttpServletRequest request, HttpSession session) {
		String error = "";
		try {
			// 别名
			String upname = File.separator + "FileUpload" + File.separator + "systemClassification";

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
				error = this.BatchImport(file, 1, session);
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

	@Override
	public Object[] categorys(SystemClassification systemClassification) {
		Object[] categorys = {};
		if (Common.isNotEmpty(systemClassification)) {
			List<Category> list = systemClassification.getCategorys();
			if (Common.isNotEmpty(list)) {
				List<String> lists = new ArrayList<>();
				for (Category s : list) {
					lists.add(s.getId());
				}
				categorys = lists.toArray();
				return categorys;
			}
		}
		return categorys;
	}

	/**
	 * 根据系统分类名称来查找，如果不存在则创建一个
	 */
	@Override
	public SystemClassification findByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("name").is(name));
		SystemClassification systemClassification = this.findOneByQuery(query, SystemClassification.class);
		if(Common.isEmpty(systemClassification)){
			SystemClassification ca = new SystemClassification();
			ca.setName(name);
			ca.setCategorys(new ArrayList<Category>());
			this.insert(ca);
			return ca;
		}
		return systemClassification; 
	}

}
