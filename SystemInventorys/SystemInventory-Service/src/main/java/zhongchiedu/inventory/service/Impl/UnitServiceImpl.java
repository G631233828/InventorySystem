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
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.CategoryService;
import zhongchiedu.inventory.service.UnitService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class UnitServiceImpl extends GeneralServiceImpl<Unit> implements UnitService {

	@Override
	@SystemServiceLog(description="编辑单位信息")
	public void saveOrUpdate(Unit unit) {
		if (Common.isNotEmpty(unit)) {
			if (Common.isNotEmpty(unit.getId())) {
				// update
				Unit ed = this.findOneById(unit.getId(), Unit.class);
				BeanUtils.copyProperties(unit, ed);
				this.save(unit);
				log.info("修改成功");
			} else {
				// insert
				this.insert(unit);
				log.info("添加成功");
			}
		}
	}


	@Override
	@SystemServiceLog(description="获取所有非禁用单位信息")
	public List<Unit> findAllUnit(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Unit.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description="删除单位信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Unit de = this.findOneById(edid, Unit.class);
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
	@SystemServiceLog(description="分页查询单位信息")
	public Pagination<Unit> findpagination(Integer pageNo, Integer pageSize) {
		// 分页查询数据
		Pagination<Unit> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Unit.class);
			if (pagination == null)
				pagination = new Pagination<Unit>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	@SystemServiceLog(description="根据名称获取单位信息")
	public BasicDataResult ajaxgetRepletes(String name) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			Unit unit = this.findOneByQuery(query, Unit.class);
			 return unit != null ?BasicDataResult.build(206,"当前货架信息已经存在，请检查", null): BasicDataResult.ok();
		}
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
	}

	@Override
	public BasicDataResult todisable(String id) {
		
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Unit unit = this.findOneById(id, Unit.class);
		if(unit == null){
			return BasicDataResult.build(400, "无法获取到企业信息，该用户可能已经被删除", null);
		}
		unit.setIsDisable(unit.getIsDisable().equals(true)?false:true);
		this.save(unit);
		
		return BasicDataResult.build(200, unit.getIsDisable().equals(true)?"禁用成功":"启用成功",unit.getIsDisable());
		
	}
	
	
	@SystemServiceLog(description="批量导入单位信息")
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
			Unit importUnit = new Unit();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {
				importUnit.setName(resultexcel[i][j]);
				
				query.addCriteria(Criteria.where("name").is(importUnit.getName()));
				query.addCriteria(Criteria.where("isDelete").is(false));
				// 通过类目名称是否存在该信息
				Unit unit = this.findOneByQuery(query, Unit.class);
				if(Common.isNotEmpty(unit)){
					error += "<span class='entypo-attention'></span>导入文件过程中出现已经存在的单位信息，第<b>&nbsp;&nbsp;" + (i + 1)
							+ "&nbsp&nbsp</b>行出现重复内容为<b>&nbsp&nbsp导入类目名称为:<b>&nbsp;&nbsp;" + unit.getName()
							+ "&nbsp;&nbsp;请手动去修改该条信息！</b></br>";
					continue;
				}else{
					this.insert(importUnit);
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
	@SystemServiceLog(description="上传单位信息")
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
	 * 根据单位名称查找单位，如果没有则创建一个
	 */
	@Override
	@SystemServiceLog(description="根据名称获取单位信息")
	public Unit findByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("isDelete").is(false));
		Unit unit = this.findOneByQuery(query, Unit.class);
		if(Common.isEmpty(unit)){
			Unit ca = new Unit();
			ca.setName(name);
			this.insert(ca);
			return ca;
		}
		return unit;
	}
	

}
