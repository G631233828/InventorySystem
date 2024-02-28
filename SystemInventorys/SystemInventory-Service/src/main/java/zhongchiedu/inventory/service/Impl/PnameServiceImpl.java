package zhongchiedu.inventory.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.NewCustomer;
import zhongchiedu.inventory.pojo.Pname;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.service.PnameService;
import zhongchiedu.log.annotation.SystemServiceLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PnameServiceImpl extends GeneralServiceImpl<Pname> implements PnameService {

	@Autowired
	private NewCustomerServiceImpl newCustomerService;

	@Override
	@SystemServiceLog(description="编辑项目名称信息")
	public void saveOrUpdate(Pname newName,String types) {
		if (Common.isNotEmpty(newName)) {
			if(Common.isNotEmpty(types)){
				List<String> ids = Arrays.asList(types.split(","));
				Query query = new Query();
				query.addCriteria(Criteria.where("_id").in(ids));
				List<NewCustomer> list=this.newCustomerService.find(query,NewCustomer.class);
				newName.setCustomers(list);
			}
			if (Common.isNotEmpty(newName.getId())) {
				// update
				Pname ed = this.findOneById(newName.getId(), Pname.class);
				BeanUtils.copyProperties(newName, ed);
				this.save(newName);
				log.info("修改成功");
			} else {
				// insert
				this.insert(newName);
				log.info("添加成功");
			}
		}
	}


	@Override
	@SystemServiceLog(description="查询所有项目名称信息")
	public List<Pname> findAllName(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Pname.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description="删除项目名称信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Pname de = this.findOneById(edid, Pname.class);
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
	@SystemServiceLog(description="查询项目名称信息")
	public Pagination<Pname> findpagination(Integer pageNo, Integer pageSize,String search) {
		// 分页查询数据
		Pagination<Pname> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			if(Common.isNotEmpty(search)) {
				query.addCriteria(Criteria.where("name").regex(search));
			}

			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Pname.class);
			if (pagination == null)
				pagination = new Pagination<Pname>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	@SystemServiceLog(description="查询重复项目名称信息")
	public BasicDataResult checkIfNameExists(String name,String fieldName) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where(fieldName).is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			Pname brand = this.findOneByQuery(query, Pname.class);
			 return brand != null ?BasicDataResult.build(206,"当前信息已经存在，请检查", null): BasicDataResult.ok();
		}
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
	}


	@Override
	@SystemServiceLog(description="模糊查询名字")
	public  BasicDataResult ajaxgetName(String abs){
		if (Common.isNotEmpty(abs)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").regex(abs));
			List<Pname> cs=this.find(query,Pname.class);
			if(cs!=null){
				String[] names=getName(cs);
				return BasicDataResult.build(200,"有数据", names);
			}else{
				BasicDataResult.build(206,"", null);
			}

		}
		return BasicDataResult.build(400,"未能获取到请求的信息",null);
	}

	private String[] getName(List<Pname> cs){
		return 	cs.stream().map(s->s.getName()).toArray(String[]::new);
	}

	@Override
	@SystemServiceLog(description="禁用项目名称信息")
	public BasicDataResult todisable(String id) {

		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Pname newName = this.findOneById(id, Pname.class);
		if(newName == null){
			return BasicDataResult.build(400, "无法获取到项目名称信息，该用户可能已经被删除", null);
		}
		newName.setIsDisable(newName.getIsDisable().equals(true)?false:true);
		this.save(newName);

		return BasicDataResult.build(200, newName.getIsDisable().equals(true)?"禁用成功":"启用成功",newName.getIsDisable());

	}







	/**
	 * 根据单位名称查找单位，如果没有则创建一个
	 */
	@Override
	@SystemServiceLog(description="根据名称查询项目名称信息")
	public Pname findByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("isDelete").is(false));
		Pname brand = this.findOneByQuery(query, Pname.class);
		if(Common.isEmpty(brand)){
			Pname ca = new Pname();
			ca.setName(name);
			this.insert(ca);
			return ca;
		}
		return brand;
	}

	@Override
	public List findIdsByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(name));
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));

		List<Pname> newNames = this.find(query, Pname.class);

		List list = new ArrayList();
		newNames.forEach(newName->{
			list.add(new ObjectId(newName.getId()));

		});
		return list;
	}


	@SystemServiceLog(description="批量导入")
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
		for (int i =1; i < rowLength; i++) {
			Query query = new Query();
			Pname newName = new Pname();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {
				newName.setName(resultexcel[i][j]);

				query.addCriteria(Criteria.where("name").is(newName.getName()));
				query.addCriteria(Criteria.where("isDelete").is(false));
				// 通过类目名称是否存在该信息
				Pname fnewName = this.findOneByQuery(query, Pname.class);
				if(Common.isNotEmpty(fnewName)){
					error += "<span class='entypo-attention'></span>导入文件过程中出现已经存在的单位信息，第<b>&nbsp;&nbsp;" + (i + 1)
							+ "&nbsp&nbsp</b>行出现重复内容为<b>&nbsp&nbsp导入类目名称为:<b>&nbsp;&nbsp;" + fnewName.getName()
							+ "&nbsp;&nbsp;请手动去修改该条信息！</b></br>";
					continue;
				}else{
					this.insert(newName);
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
	@SystemServiceLog(description="上传项目名称信息")
	public String upload(HttpServletRequest request, HttpSession session){
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

	@Override
	public Object[] newcustomerids(Pname pname) {
		Object[] ids={};
		if(Common.isNotEmpty(pname)){
			List<NewCustomer> list=pname.getCustomers();
			if(Common.isNotEmpty(list)){
				List<String> lists= list.stream()
						.map(NewCustomer::getId) // 假设 getNewcustomerid 是获取 newcustomerid 属性的方法
						.collect(Collectors.toList());
				ids=lists.toArray();
			}
		}
		return ids;
	}


}
