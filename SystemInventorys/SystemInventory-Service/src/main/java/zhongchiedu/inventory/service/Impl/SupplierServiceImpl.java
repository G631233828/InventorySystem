package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.bson.types.ObjectId;
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
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.service.SupplierService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class SupplierServiceImpl extends GeneralServiceImpl<Supplier> implements SupplierService {

	private @Autowired SystemClassificationServiceImpl systemClassificationService;
	
	private @Autowired BrandServiceImpl brandService;
	
	private @Autowired CategoryServiceImpl categoryService;
	
	@Override
	@SystemServiceLog(description="编辑供应商信息")
	public void saveOrUpdate(Supplier supplier, String types) {
		if (Common.isNotEmpty(supplier)) {
			if (Common.isNotEmpty(types)) {
				List<String> ids = Arrays.asList(types.split(","));
				Query query = new Query();
				query.addCriteria(Criteria.where("_id").in(ids));
				List<Category> list = this.categoryService.find(query, Category.class);
				supplier.setCategorys(list);
			}
			if (Common.isNotEmpty(supplier.getId())) {
				// update
				Supplier ed = this.findOneById(supplier.getId(), Supplier.class);
				BeanUtils.copyProperties(supplier, ed);
				this.save(supplier);
			} else {
				// insert
				this.insert(supplier);
			}
		}
	}

	@Override
	@SystemServiceLog(description="启用禁用供应商信息")
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Supplier supplier = this.findOneById(id, Supplier.class);
		if (Common.isEmpty(supplier)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		supplier.setIsDisable(supplier.getIsDisable().equals(true) ? false : true);
		this.save(supplier);
		return BasicDataResult.build(200, supplier.getIsDisable().equals(true) ? "禁用成功" : "恢复成功",
				supplier.getIsDisable());
	}

	@Override
	@SystemServiceLog(description="查询所有非禁用供应商信息")
	public List<Supplier> findAllSupplier(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Supplier.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description="删除供应商信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Supplier de = this.findOneById(edid, Supplier.class);
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
	@SystemServiceLog(description="分页查询供应商信息")
	public Pagination<Supplier> findpagination(Integer pageNo, Integer pageSize, String search) {
		// 分页查询数据
		Pagination<Supplier> pagination = null;
		try {
			Query query = new Query();
			
			if(Common.isNotEmpty(search)){
				query= this.findbySearch(search, query);
			}
			query.addCriteria(Criteria.where("isDelete").is(false));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Supplier.class);
			if (pagination == null)
				pagination = new Pagination<Supplier>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}
	
	
	@SystemServiceLog(description="查询供应商信息")
	public Query findbySearch(String search, Query query) {
		if (Common.isNotEmpty(search)) {
			List<Object> systemClassification = this.findSystemClassificationIds(search);
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(Criteria.where("wechat").regex(search),
					Criteria.where("qq").regex(search), Criteria.where("afterSaleService").regex(search),
					Criteria.where("introducer").regex(search), Criteria.where("name").regex(search),
					Criteria.where("address").regex(search), Criteria.where("email").regex(search),
					Criteria.where("contact").regex(search), Criteria.where("contactNumber").regex(search),
					Criteria.where("systemClassification.$id").in(systemClassification)));
		}

		return query;

	}

	/**
	 * 模糊匹配系统分类的Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description="查询系统分类信息")
	public List<Object> findSystemClassificationIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<SystemClassification> lists = this.systemClassificationService.find(query, SystemClassification.class);
		for (SystemClassification li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;

	}
	
	
	
	
	
	
	
	
	
	

	@Override
	@SystemServiceLog(description="查询重复供应商信息")
	public BasicDataResult ajaxgetRepletes(String name) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			Supplier supplier = this.findOneByQuery(query, Supplier.class);
			 return supplier != null ?BasicDataResult.build(206,"当前供应商信息已经存在，请检查", null): BasicDataResult.ok();
		}
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
	}

	@Override
	public BasicDataResult todisable(String id) {
		
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Supplier supplier = this.findOneById(id, Supplier.class);
		if(supplier == null){
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		supplier.setIsDisable(supplier.getIsDisable().equals(true)?false:true);
		this.save(supplier);
		
		return BasicDataResult.build(200, supplier.getIsDisable().equals(true)?"禁用成功":"启用成功",supplier.getIsDisable());
		
	}
	
	
	@SystemServiceLog(description="批量导入供应商信息")
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
			Supplier importSupplier = new Supplier();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {
				SystemClassification systemClassification = null;//系统分类
				Brand brand = null;//品牌
				String name = resultexcel[i][j].trim();//供应商名称
				if(Common.isEmpty(name)){
					error += "<span class='entypo-attention'></span>导入文件过程中出现供应商名称为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importSupplier.setName(name); //供应商名称
				//根据供应商名称查看是否存在，如果不存在则创建一个
				Supplier supplier = this.findByName(importSupplier.getName());
				//系统分类
				String systemClassificationName = resultexcel[i][j+1];
				if(Common.isNotEmpty(systemClassificationName)){
					systemClassification = this.systemClassificationService.findByName(systemClassificationName);
					importSupplier.setSystemClassification(systemClassification);
				}
				//类目
				List<String> categorys = Arrays.asList(resultexcel[i][j+2].split("[,，、|/]"));
				//品牌
				String brandName =  resultexcel[i][j+3];
				if(Common.isNotEmpty(brandName)){
					brand = this.brandService.findByName(brandName);
					importSupplier.setBrand(brand);
				}
				importSupplier.setContact(resultexcel[i][j+4]);
				importSupplier.setContactNumber(resultexcel[i][j+5]);
				importSupplier.setQq(resultexcel[i][j+6]);
				importSupplier.setWechat(resultexcel[i][j+7]);
				importSupplier.setEmail(resultexcel[i][j+8]);
				importSupplier.setAddress(resultexcel[i][j+9]);
				importSupplier.setIntroducer(resultexcel[i][j+10]);
				importSupplier.setProductQuality(resultexcel[i][j+11]);
			/*	importSupplier.setPrice(resultexcel[i][j+12]);*/
				importSupplier.setSupplyPeriod(resultexcel[i][j+12]);
				importSupplier.setPayMent(resultexcel[i][j+13]);
				importSupplier.setAfterSaleService(resultexcel[i][j+14]);
				
				if(Common.isNotEmpty(supplier)){
					//供应商已存在
					List<Category> list = supplier.getCategorys();
					
					for(String c : categorys){
						Category category = null;
						if(Common.isNotEmpty(c)){
							category = this.categoryService.findByName(c);
						}
						if(list.contains(category)){
//							error += "<span class='entypo-attention'></span>导入文件过程中出现已经在相同系统分类下的类目信息，第<b>&nbsp&nbsp" + (i + 1)
//									+ "&nbsp&nbsp</b>行出现重复内容为<b>&nbsp&nbsp导入类目名称为:<b>&nbsp&nbsp" + c
//									+ "&nbsp&nbsp</b>已过滤重复数据！&nbsp&nbsp</b></br>";
							continue;
						}
						list.add(category);
					}
					//更新原先供应商信息
					String newContact = importSupplier.getContact();
					String newContactNumber = importSupplier.getContactNumber();
					String newQq = importSupplier.getQq();
					String newWechat = importSupplier.getWechat();
					String newemail = importSupplier.getEmail();
					
					if(!supplier.getContact().contains(newContact)){
						supplier.setContact(Common.isNotEmpty(newContact)?Common.isNotEmpty(supplier.getContact())?supplier.getContact()+"/"+newContact:newContact:"");
						supplier.setContactNumber(Common.isNotEmpty(newContactNumber)?Common.isNotEmpty(supplier.getContactNumber())?supplier.getContactNumber()+"/"+newContactNumber:newContactNumber:"");
						supplier.setQq(Common.isNotEmpty(newQq)?Common.isNotEmpty(supplier.getQq())?supplier.getQq()+"/"+newQq:newQq:"");
						supplier.setWechat(Common.isNotEmpty(newWechat)?Common.isNotEmpty(supplier.getWechat())?supplier.getWechat()+"/"+newWechat:newWechat:"");
						supplier.setEmail(Common.isNotEmpty(newemail)?Common.isNotEmpty(supplier.getEmail())?supplier.getWechat()+"/"+newemail:newemail:"");
						error += "<span class='entypo-attention'></span>导入文件过程中出现存在的供应商信息，第<b>&nbsp&nbsp" + (i + 1)
								+ "&nbsp&nbsp</b>已经修改联系人信息<b>&nbsp&nbsp</b></br>";
					}
					
					this.save(supplier);
				}else{
					//添加新供应商
					List<Category> list = new ArrayList<>();
					for(String c : categorys){
						Category category = null;
						if(Common.isNotEmpty(c)){
							category = this.categoryService.findByName(c);
						}
						list.add(category);
					}
					importSupplier.setCategorys(list);
					this.insert(importSupplier);
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
	@SystemServiceLog(description="上传供应商信息")
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
	 * 根据单位名称查找单位
	 */
	@Override
	@SystemServiceLog(description="根据名称查询供应商信息")
	public Supplier findByName(String name) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("isDelete").is(false));
		Supplier supplier = this.findOneByQuery(query, Supplier.class);
		/*if(Common.isEmpty(supplier)){
			Supplier ca = new Supplier();
			ca.setName(name);
			this.insert(ca);
			return ca;
		}*/
		return supplier;
	}

	@Override
	@SystemServiceLog(description="获取所有供应商id")
	public Object[] categorys(Supplier supplier) {
		Object[] categorys = {};
		if (Common.isNotEmpty(supplier)) {
			List<Category> list = supplier.getCategorys();
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

	@Override
	@SystemServiceLog(description="根据id查询供应商信息")
	public BasicDataResult findOneById(String id) {
		Supplier supplier = this.findOneById(id, Supplier.class);
		return Common.isNotEmpty(supplier)?BasicDataResult.build(200, "查询成功", supplier):BasicDataResult.build(400, "查询失败", null);
	}
	
	
	
	
	
	
	
	

}
