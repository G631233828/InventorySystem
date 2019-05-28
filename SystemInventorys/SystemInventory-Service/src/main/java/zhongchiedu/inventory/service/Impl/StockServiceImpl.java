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
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
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
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.StockService;

@Service
@Slf4j
public class StockServiceImpl extends GeneralServiceImpl<Stock> implements StockService {

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired SystemClassificationServiceImpl systemClassificationService;

	private @Autowired CategoryServiceImpl categoryService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired BrandServiceImpl brandService;

	@Override
	public void saveOrUpdate(Stock stock) {
		if (Common.isNotEmpty(stock)) {
			if (Common.isNotEmpty(stock.getId())) {
				// update
				Stock ed = this.findOneById(stock.getId(), Stock.class);
				BeanUtils.copyProperties(stock, ed);
				this.save(stock);
			} else {
				// insert
				this.insert(stock);
			}
		}
	}

	@Override
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Stock stock = this.findOneById(id, Stock.class);
		if (Common.isEmpty(stock)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);
		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "恢复成功", stock.getIsDisable());
	}

	@Override
	public List<Stock> findAllStock(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Stock.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Stock de = this.findOneById(edid, Stock.class);
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

	/**
	 * Criteria cr = new Criteria(); Criteria ca1 = null; Criteria ca2 = null;
	 * ca1 = Criteria.where("name").regex(serach); ca2 =
	 * Criteria.where("code").regex(serach);
	 * stu.addCriteria(Criteria.where("clazz.$id").is(new
	 * ObjectId(clazz.getId()))); stu.addCriteria(cr.orOperator(ca1, ca2));
	 * List<Student> list = this.studentService.find(stu, Student.class); List
	 * listids = new ArrayList(); if (list.size() > 0) { for (Student s : list)
	 * { listids.add(new ObjectId(s.getId())); } } Criteria cr1 = new
	 * Criteria(); Criteria ca3 = null; Criteria ca4 = null; ca3 =
	 * Criteria.where("startLeave").regex(serach); ca4 =
	 * Criteria.where("student.$id").in(listids);
	 * query.addCriteria(cr1.orOperator(ca3, ca4));
	 */

	@Override
	public Pagination<Stock> findpagination(Integer pageNo, Integer pageSize, String search) {
		// 分页查询数据
		Pagination<Stock> pagination = null;
		try {
			Query query = new Query();
			query = this.findbySearch(search, query);
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Stock.class);
			if (pagination == null)
				pagination = new Pagination<Stock>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	public Query findbySearch(String search, Query query) {
		if (Common.isNotEmpty(search)) {
			List<Object> systemClassification = this.findSystemClassificationIds(search);
			List<Object> brand = this.findBrandIds(search);
			List<Object> goodsStorage = this.findGoodsStorageIds(search);
			List<Object> category = this.findCategoryIds(search);
			List<Object> suppliersId = this.findSuppliersId(search, systemClassification, category, brand);
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(Criteria.where("goodsStorage.$id").in(goodsStorage),
					Criteria.where("supplier.$id").in(suppliersId),Criteria.where("name").regex(search),
					Criteria.where("model").regex(search), Criteria.where("scope").regex(search)));
		} 
			query.addCriteria(Criteria.where("isDelete").is(false));
		
		return query;

	}

	/**
	 * 模糊匹配系统分类的Id
	 * 
	 * @param search
	 * @return
	 */
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

	/**
	 * 模糊匹配类目的Id
	 * 
	 * @param search
	 * @return
	 */
	public List<Object> findBrandIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Brand> lists = this.brandService.find(query, Brand.class);
		for (Brand li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	/**
	 * 模糊匹配货架号的Id
	 * 
	 * @param search
	 * @return
	 */
	public List<Object> findGoodsStorageIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();
		query.addCriteria(ca.orOperator(Criteria.where("address").regex(search),
				Criteria.where("shelfNumber").regex(search), Criteria.where("shelflevel").regex(search)));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<GoodsStorage> lists = this.goodsStorageService.find(query, GoodsStorage.class);
		for (GoodsStorage li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	/**
	 * 模糊匹配品牌的Id
	 * 
	 * @param search
	 * @return
	 */
	public List<Object> findCategoryIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Category> lists = this.categoryService.find(query, Category.class);
		for (Category li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	/**
	 * 模糊匹配查过供应商以及供应商设备信息Id
	 * 
	 * @param search
	 * @return
	 */
	public List<Object> findSuppliersId(String search, List<Object> systemClassification, List<Object> categorys,
			List<Object> brands) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();
		query.addCriteria(ca.orOperator(Criteria.where("price").regex(search), Criteria.where("wechat").regex(search),
				Criteria.where("qq").regex(search), Criteria.where("afterSaleService").regex(search),
				Criteria.where("introducer").regex(search), Criteria.where("name").regex(search),
				Criteria.where("address").regex(search), Criteria.where("email").regex(search),
				Criteria.where("contact").regex(search), Criteria.where("contactNumber").regex(search),
				Criteria.where("systemClassification.$id").in(systemClassification),
				Criteria.where("categorys.$id").in(categorys), Criteria.where("brand.$id").in(brands)));

		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Supplier> lists = this.supplierService.find(query, Supplier.class);
		for (Supplier li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@Override
	public BasicDataResult ajaxgetRepletes(String name) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			Stock stock = this.findOneByQuery(query, Stock.class);
			return stock != null ? BasicDataResult.build(206, "当前供应商信息已经存在，请检查", null) : BasicDataResult.ok();
		}
		return BasicDataResult.build(400, "未能获取到请求的信息", null);
	}

	@Override
	public BasicDataResult todisable(String id) {

		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Stock stock = this.findOneById(id, Stock.class);
		if (stock == null) {
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);

		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "启用成功", stock.getIsDisable());

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
			Stock importStock = new Stock();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {

				Stock stock = null; // 库存信息
				Supplier supplier = null;
				Unit unit = null;
				String name = resultexcel[i][j].trim();// 设备名称
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备名称为空，第<b>&nbsp&nbsp" + (i + 2)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importStock.setName(name); // 设备名称
				String model = resultexcel[i][j + 1].trim();
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备型号为空，第<b>&nbsp&nbsp" + (i + 2)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importStock.setModel(model);// 设备型号
				importStock.setScope(resultexcel[i][j + 2].trim());// 使用范围
				importStock.setPrice(resultexcel[i][j + 3].trim());// 价格

				String unitName = resultexcel[i][j + 4].trim();
				if (Common.isNotEmpty(unitName)) {
					// 根据供应商名称查找，看供应商是否存在
					unit = this.unitService.findByName(unitName);
				}
				importStock.setUnit(unit);
				importStock.setMaintenance(resultexcel[i][j + 5].trim());// 维保
				String supplierName = resultexcel[i][j + 6].trim();// 供应商名称
				if (Common.isNotEmpty(supplierName)) {
					// 根据供应商名称查找，看供应商是否存在
					supplier = this.supplierService.findByName(supplierName);
					if (Common.isEmpty(supplier)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的供应商<b>&nbsp;&nbsp;" + supplierName
								+ "&nbsp;&nbsp;</b>，请先添加供应商，第<b>&nbsp&nbsp" + (i + 2)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				importStock.setSupplier(supplier);

				stock = this.findByName(name, model);
				if (Common.isNotEmpty(stock)) {
					// 设备已存在
					error += "<span class='entypo-attention'></span>导入文件过程中设备已经存在，设备名称<b>&nbsp;&nbsp;" + stock.getName()
							+ "&nbsp;&nbsp;</b>，第<b>&nbsp&nbsp" + (i + 2) + "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				} else {
					// 添加新设备
					this.insert(importStock);
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

	/**
	 * 根据单位名称查找单位，如果没有则创建一个
	 */
	@Override
	public Stock findByName(String name, String model) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("model").is(model));
		query.addCriteria(Criteria.where("isDelete").is(false));
		Stock stock = this.findOneByQuery(query, Stock.class);
		/*
		 * if(Common.isEmpty(stock)){ Stock ca = new Stock(); ca.setName(name);
		 * this.insert(ca); return ca; }
		 */
		return stock;
	}

}
