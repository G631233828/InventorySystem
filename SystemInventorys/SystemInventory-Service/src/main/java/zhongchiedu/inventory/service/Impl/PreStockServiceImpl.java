package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.*;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.*;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.PreStockService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class PreStockServiceImpl extends GeneralServiceImpl<PreStock> implements PreStockService {

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired SystemClassificationServiceImpl systemClassificationService;

	private @Autowired CategoryServiceImpl categoryService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired BrandServiceImpl brandService;

	private @Autowired AreaService areaService;

	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	@SystemServiceLog(description = "编辑库存信息")
	public void saveOrUpdate(PreStock stock) {
		if (Common.isNotEmpty(stock)) {
			if (Common.isNotEmpty(stock.getId())) {
				// update
				PreStock ed = this.findOneById(stock.getId(), PreStock.class);
				BeanUtils.copyProperties(stock, ed);
				stock.setUpdateTime(new Date());
				this.save(stock);
			} else {
				// insert
				this.insert(stock);
			}
		}
	}

	@Override
	@SystemServiceLog(description = "获取所有非禁用库存信息")
	public List<PreStock> findAllPreStock(boolean isdisable, String areaId) {
		Query query = new Query();
		if (Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, PreStock.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除库存信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				PreStock de = this.findOneById(edid, PreStock.class);
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
	@SystemServiceLog(description = "分页查询库存信息")
	public Pagination<PreStock>
	findpagination(Integer pageNo, Integer pageSize, String search, String searchArea,
			int status,String ssC) {
		// 分页查询数据
		Pagination<PreStock> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(searchArea)) {
				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			}
			query.addCriteria(Criteria.where("status").is(status));

			if (Common.isNotEmpty(ssC)) {
				query = query.addCriteria(Criteria.where("systemClassification.$id").is(new ObjectId(ssC)));
			}

			if (Common.isNotEmpty(search)) {
				query = this.findbySearch(search, query);

			}
			query.addCriteria(Criteria.where("isDelete").is(false));
			 query.with(new Sort(new Order(Direction.DESC, "createTime")));
			query.with(new Sort(new Order(Direction.DESC, "inventory"))); //按照库存量排序

			pagination = this.findPaginationByQuery(query, pageNo, pageSize, PreStock.class);
			if (pagination == null)
				pagination = new Pagination<PreStock>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	public Pagination<PreStock> findpagination(Integer pageNo, Integer pageSize, RequestBo requestBo,Integer status){
		Pagination<PreStock> pagination = null;
		try {
			Query query = new Query();
			Criteria ca = new Criteria();
			if(Common.isNotEmpty(requestBo.getName())){
				query=query.addCriteria(Criteria.where("name").regex(requestBo.getName(), "i"));
			}
			if(Common.isNotEmpty(requestBo.getEntryName())){
				query=query.addCriteria(Criteria.where("entryName").regex(requestBo.getEntryName(), "i"));
			}
			if(Common.isNotEmpty(requestBo.getModel())){
				query=query.addCriteria(Criteria.where("model").regex(requestBo.getModel(), "i"));
			}
			if (Common.isNotEmpty(requestBo.getSsC())) {
				String[] ssCs=requestBo.getSsC().split(",");
//				query = query.addCriteria(Criteria.where("systemClassification.$id").is(new ObjectId(ssC)));
				query = query.addCriteria(Criteria.where("systemClassification.$id").in(Arrays.stream(ssCs).map(str->new ObjectId(str)).collect(Collectors.toList())));
			}
			if (Common.isNotEmpty(requestBo.getSearchArea())) {
				String[] sas=requestBo.getSearchArea().split(",");
//				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
				query = query.addCriteria(Criteria.where("area.$id").in(Arrays.stream(sas).map(str->new ObjectId(str)).collect(Collectors.toList())));
			}
			if(Common.isNotEmpty(requestBo.getItemNo())){
				query=query.addCriteria(Criteria.where("itemNo").regex(requestBo.getItemNo(), "i"));
			}

			if(Common.isNotEmpty(requestBo.getPurchaseInvoiceNo())){
				query=query.addCriteria(Criteria.where("purchaseInvoiceNo").regex(requestBo.getPurchaseInvoiceNo(), "i"));
			}
			if(Common.isNotEmpty(requestBo.getPaymentOrderNo())){
				query=query.addCriteria(Criteria.where("paymentOrderNo").regex(requestBo.getPaymentOrderNo(), "i"));
			}
			if(Common.isNotEmpty(requestBo.getPurchaseInvoiceDate())){
				query=query.addCriteria(Criteria.where("purchaseInvoiceDate").regex(requestBo.getPurchaseInvoiceDate(), "i"));
			}
			if(Common.isNotEmpty(requestBo.getSupplier())){
				Query squery=new Query();
				squery.addCriteria(Criteria.where("name").regex(requestBo.getSupplier(),"i"));
				List<Supplier> supplierList = this.supplierService.find(squery, Supplier.class);
				if(!supplierList.isEmpty()){
					ca.orOperator(Criteria.where("supplier.$id").in(supplierList.stream().map(supplier ->new ObjectId(supplier.getId())).collect(Collectors.toList())));
					query.addCriteria(ca);
				}
			}
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.addCriteria(Criteria.where("status").is(status));
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
			query.with(new Sort(new Order(Direction.DESC, "inventory"))); //按照库存量排序
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, PreStock.class);
			if (pagination == null)
				pagination = new Pagination<PreStock>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@SystemServiceLog(description = "查询库存信息")
	public Query findbySearch(String search, Query query) {
		if (Common.isNotEmpty(search)) {
			 List<Object> systemClassification = this.findSystemClassificationIds(search);
			 List<Object> brand = this.findBrandIds(search);
//			 List<Object> goodsStorage = this.findGoodsStorageIds(search);
			 List<Object> category = this.findCategoryIds(search);
			 List<Object> suppliersId = this.findSuppliersId(search, systemClassification,
			 category, brand);
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(/*
											 * Criteria.where("goodsStorage.$id").in(goodsStorage), */
											 Criteria.where("supplier.$id").in(suppliersId),
											 Criteria.where("name").regex(search),
					Criteria.where("model").regex(search),
					Criteria.where("entryName").regex(search),
					Criteria.where("purchaseInvoiceNo").regex(search),
					Criteria.where("paymentOrderNo").regex(search),
					Criteria.where("itemNo").regex(search),
					Criteria.where("scope").regex(search)));
		}

		return query;

	}



	/**
	 * 模糊匹配系统分类的Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description = "查询系统分类信息")
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
	@SystemServiceLog(description = "查询品牌信息")
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
	@SystemServiceLog(description = "查询货架信息")
	public List<Object> findGoodsStorageIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();
		if (search.contains("/")) {
			query.addCriteria(ca.orOperator(Criteria.where("address").regex(search),
					Criteria.where("shelfNumber").is(search.split("/")[0]),
					Criteria.where("shelflevel").is(search.split("/")[1])));
		} else {
			query.addCriteria(ca.orOperator(Criteria.where("address").regex(search),
					Criteria.where("shelfNumber").regex(search), Criteria.where("shelflevel").regex(search)));
		}

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
	@SystemServiceLog(description = "查询类目信息")
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
	@SystemServiceLog(description = "查询供应商信息")
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
	@SystemServiceLog(description = "启用禁用库存信息")
	public BasicDataResult todisable(String id) {

		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		PreStock stock = this.findOneById(id, PreStock.class);
		if (stock == null) {
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);

		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "启用成功", stock.getIsDisable());

	}

	@SystemServiceLog(description = "批量导入预库存信息")
	public String BatchImport(File file, int row, HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);

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
			PreStock importPreStock = new PreStock();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {

				PreStock stock = null; // 库存信息
				Supplier supplier = null;
				Unit unit = null;
				SystemClassification ssC=null;
				String areaName = resultexcel[i][j].trim();// 区域名称
				// 通过区域名称查询区域是否存在
				Area getarea = this.areaService.findByName(areaName);
				if (Common.isEmpty(getarea)) {
					error += "<span class='entypo-attention'></span>导入文件过程中，第<b>&nbsp&nbsp" + (i + 1)
							+ "行出现未添加的区域，请手动去修改该条信息或创建区域！&nbsp&nbsp</b></br>";
					return error;
				}
				importPreStock.setArea(getarea);
				String name = resultexcel[i][j + 1].trim();// 设备名称
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备名称为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importPreStock.setName(name);
				String model = resultexcel[i][j + 2].trim();// 设备型号
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备型号为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importPreStock.setModel(model);
				importPreStock.setEstimatedInventoryQuantity(Long.valueOf(resultexcel[i][j + 3].trim()));// 预备入库的数量
				importPreStock.setEntryName(resultexcel[i][j + 5].trim());// 项目名称
				String entryName=resultexcel[i][j + 5].trim();
				String unitName = resultexcel[i][j + 4].trim();//单位
				if (Common.isNotEmpty(unitName)) {
					// 根据供应商名称查找，看供应商是否存在
					unit = this.unitService.findByName(unitName);
				}
				importPreStock.setUnit(unit);
//				importPreStock.setMaintenance(resultexcel[i][j + 6].trim()); 维保
				String supplierName = resultexcel[i][j + 6].trim();// 供应商名称
				if (Common.isNotEmpty(supplierName)) {
					// 根据供应商名称查找，看供应商是否存在
					supplier = this.supplierService.findByName(supplierName);
					if (Common.isEmpty(supplier)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的供应商<b>&nbsp;&nbsp;" + supplierName
								+ "&nbsp;&nbsp;</b>，请先添加供应商，第<b>&nbsp&nbsp" + (i + 1)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				String ssCName = resultexcel[i][j + 7].trim();// 系统分类
				if (Common.isNotEmpty(ssCName)) {
					// 根据供应商名称查找，看供应商是否存在
					ssC = this.systemClassificationService.findByName(ssCName);
					if (Common.isEmpty(supplier)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的系统分类<b>&nbsp;&nbsp;" + ssCName
								+ "&nbsp;&nbsp;</b>，请先添加系统分类，第<b>&nbsp&nbsp" + (i + 1)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				importPreStock.setSystemClassification(ssC);
				importPreStock.setSupplier(supplier);
				importPreStock.setPublisher(user);// 发布人
				stock = this.findByName(getarea,name, model,1,entryName);//预入库查重 区域，名字，型号，项目名称

				if (Common.isNotEmpty(stock)) {
					if(Common.isNotEmpty(ssC))stock.setSystemClassification(ssC);
					long newnum=this.updatePreStock(stock,importPreStock.getEstimatedInventoryQuantity());
					error += "<span class='entypo-attention'></span>该设备预库存已经存在，预库存数量将会叠加<b>&nbsp;&nbsp;" + stock.getName()
							+ "&nbsp;&nbsp;</b>，预库存量为"+newnum+"！&nbsp&nbsp</b></br>";
					// 设备已存在
//					error += "<span class='entypo-attention'></span>该设备预库存已经存在，设备名称<b>&nbsp;&nbsp;" + stock.getName()
//							+ "&nbsp;&nbsp;</b>，第<b>&nbsp&nbsp" + (i + 1) + "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				} else {
					// 添加新设备
					this.insert(importPreStock);
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
	 * 更新预库存中的预计入库数量
	 * @param stock
	 * @param num
	 * @return
	 */
	private long updatePreStock(PreStock stock,long num){
			lock.lock();
			long newnum=0;
			try{
				newnum=stock.getEstimatedInventoryQuantity()+num;
				stock.setEstimatedInventoryQuantity(newnum);

				this.save(stock);
				return newnum;
			}finally {
				lock.unlock();
			}
		}
	/**
	 * 执行上传文件，返回错误消息
	 */
	@SystemServiceLog(description = "上传库存信息")
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
	 * 根据单位名称查找单位，如果没有则创建一个
	 */
	@Override
	public PreStock findByName(Area area,String name, String model,Integer status,String entryName) {
		Query query = new Query();
		if (Common.isNotEmpty(area.getId())) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(area.getId())));
		}
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("model").is(model));
		query.addCriteria(Criteria.where("entryName").is(entryName));
		query.addCriteria(Criteria.where("status").is(status));
		query.addCriteria(Criteria.where("isDelete").is(false));
		PreStock stock = this.findOneByQuery(query, PreStock.class);
		/*
		 * if(Common.isEmpty(stock)){ PreStock ca = new PreStock(); ca.setName(name);
		 * this.insert(ca); return ca; }
		 */
		return stock;
	}

	@SystemServiceLog(description = "根据id查询库存信息")
	public BasicDataResult findOneById(String id) {
		PreStock stock = this.findOneById(id, PreStock.class);

		return Common.isNotEmpty(stock) ? BasicDataResult.build(200, "查询成功", stock)
				: BasicDataResult.build(400, "查询失败", null);
	}

	@Override
	@SystemServiceLog(description = "导出预库存信息")
	public HSSFWorkbook export(String name, String areaId) {
		HSSFWorkbook wb = new HSSFWorkbook();

		// 创建sheet
		HSSFSheet sheet = wb.createSheet(name);
		HSSFCellStyle style = createStyle(wb);
		List<String> title = this.title();
		this.createHead(sheet, title, style, name);
		this.createTitle(sheet, title, style);
		this.createPreStock(sheet, title, style, areaId);
		sheet.setDefaultColumnWidth(12);
		sheet.autoSizeColumn(1, true);
		return wb;
	}

	/**
	 * 创建样式
	 * 
	 * @param wb
	 * @return
	 */
	public HSSFCellStyle createStyle(HSSFWorkbook wb) {

		HSSFCellStyle style = wb.createCellStyle();
		// 设置边框
		style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		style.setBorderRight(HSSFCellStyle.BORDER_THIN);
		HSSFFont font = wb.createFont();
		font.setFontName("宋体");
		font.setFontHeightInPoints((short) 9);
		style.setFont(font);
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 水平布局：居
		return style;

	}

	/**
	 * 创建第一行
	 * 
	 * @param sheet
	 */
	public void createHead(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String name) {
		HSSFRow row = sheet.createRow(0);// 初始化excel第一行
		HSSFCell cell = row.createCell(0);
		cell.setCellValue(name);
		cell.setCellStyle(style);
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, title.size() - 1));
		for (int a = 1; a < title.size(); a++) {
			cell = row.createCell(a);
			cell.setCellStyle(style);
		}
	}

	/**
	 * 创建第二行 创建所有的title
	 * 
	 * @param sheet
	 */
	public void createTitle(HSSFSheet sheet, List<String> title, HSSFCellStyle style) {
		HSSFRow row = sheet.createRow(1);
		for (int a = 0; a < title.size(); a++) {
			HSSFCell cell = row.createCell(a);
			cell.setCellValue(title.get(a));
			cell.setCellStyle(style);
		}
	}

	/**
	 * 创建第三行 创建数据
	 * 
	 * @param sheet
	 */
	public void createPreStock(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String areaId) {

//		int j = 1;
//		// 获取所有的库存
//		List<PreStock> list = this.findAllPreStock(false, areaId);
//
//		for (PreStock stock : list) {
//			// 获取所有的设备
//			HSSFRow row = sheet.createRow(j + 1);
//
//			HSSFCell cell = row.createCell(0);
//			cell.setCellStyle(style);
//			cell.setCellValue(stock.getArea().getName());
//
//			cell = row.createCell(1);
//			cell.setCellStyle(style);
//			cell.setCellValue(stock.getName());
//
//			cell = row.createCell(2);
//			cell.setCellStyle(style);
//			cell.setCellValue(stock.getModel());
//
//			cell = row.createCell(3);
//			cell.setCellStyle(style);
//			cell.setCellValue(stock.getScope());
//
//			cell = row.createCell(4);
//			cell.setCellStyle(style);
//			if (Common.isNotEmpty(stock.getGoodsStorage())) {
//				String level ="";
//				if(Common.isEmpty(stock.getGoodsStorage().getShelflevel())) {
//					level="/"+stock.getGoodsStorage().getShelflevel();
//				}
//				cell.setCellValue( stock.getGoodsStorage().getShelfNumber() +level);
//			} else {
//				cell.setCellValue("-/-");
//
//			}
//			cell = row.createCell(5);
//			cell.setCellStyle(style);
//			if (Common.isNotEmpty(stock.getUnit())) {
//				cell.setCellValue(stock.getUnit().getName());
//			} else {
//				cell.setCellValue("");
//			}
//
//			cell = row.createCell(6);
//			cell.setCellStyle(style);
//			if (Common.isNotEmpty(stock.getPrice())) {
//				cell.setCellValue(stock.getPrice());
//			} else {
//				cell.setCellValue("");
//			}
//
//			cell = row.createCell(7);
//			cell.setCellStyle(style);
//			cell.setCellValue(stock.getInventory());
//
//			cell = row.createCell(8);
//			cell.setCellStyle(style);
//			if (Common.isNotEmpty(stock.getInventory()) && Common.isNotEmpty(stock.getPrice())) {
//				cell.setCellValue(stock.getInventory() * Double.valueOf(stock.getPrice()));
//			} else {
//				cell.setCellValue("");
//			}
//
//			cell = row.createCell(9);
//			cell.setCellStyle(style);
//			cell.setCellValue(stock.isReceivables() ? "是" : "否");
//
//			j++;
//		}

	}

	public Workbook newExport(HttpServletRequest request,  RequestBo requestBo){
		Query query = new Query();
		Criteria ca = new Criteria();
		if(Common.isNotEmpty(requestBo.getName())){
			query=query.addCriteria(Criteria.where("name").regex(requestBo.getName(), "i"));
		}
		if(Common.isNotEmpty(requestBo.getEntryName())){
			query=query.addCriteria(Criteria.where("entryName").regex(requestBo.getEntryName(), "i"));
		}
		if(Common.isNotEmpty(requestBo.getModel())){
			query=query.addCriteria(Criteria.where("model").regex(requestBo.getModel(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getSsC())) {
			String[] ssCs=requestBo.getSsC().split(",");
//				query = query.addCriteria(Criteria.where("systemClassification.$id").is(new ObjectId(ssC)));
			query = query.addCriteria(Criteria.where("systemClassification.$id").in(Arrays.stream(ssCs).map(str->new ObjectId(str)).collect(Collectors.toList())));
		}
		if (Common.isNotEmpty(requestBo.getSearchArea())) {
			String[] sas=requestBo.getSearchArea().split(",");
//				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			query = query.addCriteria(Criteria.where("area.$id").in(Arrays.stream(sas).map(str->new ObjectId(str)).collect(Collectors.toList())));
		}
		if(Common.isNotEmpty(requestBo.getItemNo())){
			query=query.addCriteria(Criteria.where("itemNo").regex(requestBo.getItemNo(), "i"));
		}

		if(Common.isNotEmpty(requestBo.getPurchaseInvoiceNo())){
			query=query.addCriteria(Criteria.where("purchaseInvoiceNo").regex(requestBo.getPurchaseInvoiceNo(), "i"));
		}
		if(Common.isNotEmpty(requestBo.getPaymentOrderNo())){
			query=query.addCriteria(Criteria.where("paymentOrderNo").regex(requestBo.getPaymentOrderNo(), "i"));
		}
		if(Common.isNotEmpty(requestBo.getPurchaseInvoiceDate())){
			query=query.addCriteria(Criteria.where("purchaseInvoiceDate").regex(requestBo.getPurchaseInvoiceDate(), "i"));
		}
		if(Common.isNotEmpty(requestBo.getSupplier())){
			Query squery=new Query();
			squery.addCriteria(Criteria.where("name").regex(requestBo.getSupplier(),"i"));
			List<Supplier> supplierList = this.supplierService.find(squery, Supplier.class);
			if(!supplierList.isEmpty()){
				ca.orOperator(Criteria.where("supplier.$id").in(supplierList.stream().map(supplier ->new ObjectId(supplier.getId())).collect(Collectors.toList())));
				query.addCriteria(ca);
			}
		}
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("status").is(1));
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		query.with(new Sort(new Order(Direction.DESC, "inventory"))); //按照库存量排序
//		List<PreStock> list = this.findAllPreStock(false, areaId);
		List<PreStock> list =find(query,PreStock.class);
		List<Map<String, Object>> arrayList = new ArrayList<>();
		List<Map<String, Object>> doneList = new ArrayList<>();
		for (PreStock stock : list) {
			if(stock.getStatus() == 1){
				Map<String, Object> in = new HashMap<>();
				in.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
				in.put("name", Common.isEmpty(stock.getName()) ? "" : stock.getName());
				in.put("model", Common.isEmpty(stock.getModel()) ? "" : stock.getModel());
				in.put("esq", Common.isEmpty(stock.getEstimatedInventoryQuantity()) ? "" : stock.getEstimatedInventoryQuantity());
				in.put("arq", Common.isEmpty(stock.getActualReceiptQuantity()) ? "" : stock.getActualReceiptQuantity());
				in.put("sy",stock.getEstimatedInventoryQuantity()-stock.getActualReceiptQuantity());
				in.put("unit", Common.isEmpty(stock.getUnit()) ? "" : stock.getUnit().getName());
				arrayList.add(in);
			} else if (stock.getStatus() == 2) {
				Map<String, Object> done = new HashMap<>();
				done.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
				done.put("name", Common.isEmpty(stock.getName()) ? "" : stock.getName());
				done.put("model", Common.isEmpty(stock.getModel()) ? "" : stock.getModel());
				done.put("esq", Common.isEmpty(stock.getEstimatedInventoryQuantity()) ? "" : stock.getEstimatedInventoryQuantity());
				done.put("arq", Common.isEmpty(stock.getActualReceiptQuantity()) ? "" : stock.getActualReceiptQuantity());
				done.put("sy",stock.getEstimatedInventoryQuantity()-stock.getActualReceiptQuantity());
				done.put("unit", Common.isEmpty(stock.getUnit()) ? "" : stock.getUnit().getName());
//				上次修改时间
//				String lastTime="";
//				try {
//					lastTime=Common.isEmpty(stock.getUpdateTime())?"":Common.getDateYMDHM(stock.getUpdateTime());
//				} catch (ParseException e) {
//					throw new RuntimeException(e);
//				}
//				done.put("lastTime",lastTime);
				doneList.add(done);
			}

		}
		Map<String, Object> dataMap = new HashMap<>();

		dataMap.put("inlist", arrayList);
		dataMap.put("outlist", doneList);

		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
		String fileName = "预入库管理模板.xlsx";
		TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
		Workbook doc = null;

		try {
			doc = ExcelExportUtil.exportExcel(params, dataMap);
//							WordUtil.exportWord(ctxPath+fileName, dataMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return doc;
	}

	/**
	 * 设置title
	 * 
	 * @return
	 */
	public List<String> title() {
		List<String> list = new ArrayList<>();
		list.add("区域");
		list.add("设备名称");
		list.add("品名型号");
		list.add("使用范围");
		list.add("货架号/层");
		list.add("计量单位 ");
		list.add("单价");
		list.add("当前库存");
		list.add("总价");
		list.add("应收款");
		return list;
	}

	@Override
	public List<PreStock> findAllPreStockByStatus(boolean isdisable, int status) {
		Query query = new Query();
		if (Common.isNotEmpty(status)) {
			query.addCriteria(Criteria.where("status").is(status));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, PreStock.class);
	}

	@Override
	public List<PreStock> findStocksByIds(List ids) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").in(ids));
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		return this.find(query, PreStock.class);
	}

	@Override
	public void updateStockStatistics(String ids,Double inprice,String purchaseInvoiceNo,String purchaseInvoiceDate,String paymentOrderNo,String itemNo){
		List<String> array = Arrays.asList(ids.split(","));
		for (String id : array) {
			PreStock preStock = this.findOneById(id, PreStock.class);
			if (inprice != null) {
				preStock.setInprice(inprice);
			}
			if (!purchaseInvoiceNo.equals("null")) {
				preStock.setPurchaseInvoiceNo(purchaseInvoiceNo);
			}
//			if (!receiptNo.equals("null")) {
//				stockStatistics.setReceiptNo(receiptNo);
//			}
			if (!paymentOrderNo.equals("null")) {
				preStock.setPaymentOrderNo(paymentOrderNo);
			}
//			if (!sailesInvoiceNo.equals("null")) {
//				stockStatistics.setSailesInvoiceNo(sailesInvoiceNo);
//			}
//			if (!sailesInvoiceDate.equals("null")) {
//				stockStatistics.setSailesInvoiceDate(sailesInvoiceDate);
//			}
			if (!purchaseInvoiceDate.equals("null")) {
				preStock.setPurchaseInvoiceDate(purchaseInvoiceDate);
			}
//			if (sailPrice != null) {
//				stockStatistics.setSailPrice(sailPrice);
//			}
			if(!itemNo.equals("null")){
				preStock.setItemNo(itemNo);
			}
//			stockStatistics.setEditFinanceTime(Common.fromDateH());
//			stockStatistics.setFinanceUser(user);
			this.save(preStock);
		}
	}
}
