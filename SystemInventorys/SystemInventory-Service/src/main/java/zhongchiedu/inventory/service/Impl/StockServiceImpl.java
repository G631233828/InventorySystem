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

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.log.annotation.SystemServiceLog;

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
	@SystemServiceLog(description="编辑库存信息")
	public void saveOrUpdate(Stock stock) {
		if (Common.isNotEmpty(stock)) {
			if (Common.isNotEmpty(stock.getId())) {
				// update
				Stock ed = this.findOneById(stock.getId(), Stock.class);
				stock.setInventory(ed.getInventory());
				BeanUtils.copyProperties(stock, ed);
				this.save(stock);
			} else {
				// insert
				this.insert(stock);
			}
		}
	}

	@Override
	@SystemServiceLog(description="启用禁用库存信息")
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
	@SystemServiceLog(description="获取所有非禁用库存信息")
	public List<Stock> findAllStock(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Stock.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description="删除库存信息")
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

	@Override
	@SystemServiceLog(description="分页查询库存信息")
	public Pagination<Stock> findpagination(Integer pageNo, Integer pageSize, String search) {
		// 分页查询数据
		Pagination<Stock> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(search)) {
				query = this.findbySearch(search, query);
			}
			query.addCriteria(Criteria.where("isDelete").is(false));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Stock.class);
			if (pagination == null)
				pagination = new Pagination<Stock>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}
	@SystemServiceLog(description="查询库存信息")
	public Query findbySearch(String search, Query query) {
		if (Common.isNotEmpty(search)) {
			List<Object> systemClassification = this.findSystemClassificationIds(search);
			List<Object> brand = this.findBrandIds(search);
			List<Object> goodsStorage = this.findGoodsStorageIds(search);
			List<Object> category = this.findCategoryIds(search);
			List<Object> suppliersId = this.findSuppliersId(search, systemClassification, category, brand);
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(Criteria.where("goodsStorage.$id").in(goodsStorage),
					Criteria.where("supplier.$id").in(suppliersId), Criteria.where("name").regex(search),
					Criteria.where("model").regex(search), Criteria.where("scope").regex(search)));
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

	/**
	 * 模糊匹配类目的Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description="查询品牌信息")
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
	@SystemServiceLog(description="查询货架信息")
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
	@SystemServiceLog(description="查询类目信息")
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
	@SystemServiceLog(description="查询供应商信息")
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
	@SystemServiceLog(description="根据名称查询库存信息")
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
	@SystemServiceLog(description="启用禁用库存信息")
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

	@SystemServiceLog(description="批量导入库存信息")
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
	@SystemServiceLog(description="上传库存信息")
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
	@SystemServiceLog(description="根据id查询库存信息")
	public BasicDataResult findOneById(String id) {
		Stock stock = this.findOneById(id, Stock.class);

		return Common.isNotEmpty(stock) ? BasicDataResult.build(200, "查询成功", stock)
				: BasicDataResult.build(400, "查询失败", null);
	}

	@Override
	@SystemServiceLog(description="导出库存信息")
	public HSSFWorkbook export(String name) {
		HSSFWorkbook wb = new HSSFWorkbook();

		// 创建sheet
		HSSFSheet sheet = wb.createSheet(name);
		HSSFCellStyle style = createStyle(wb);
		List<String> title = this.title();
		this.createHead(sheet, title, style, name);
		this.createTitle(sheet, title, style);
		this.createStock(sheet, title, style);
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
		for (int a = 0; a < title.size(); a++) {
			HSSFCell cell = row.createCell(a);
			cell.setCellValue(name);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, title.size() - 1));
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
	public void createStock(HSSFSheet sheet, List<String> title, HSSFCellStyle style) {

		int j = 1;
		// 获取所有的库存
		List<Stock> list = this.findAllStock(false);

		for (Stock stock : list) {
			// 获取所有的设备
			HSSFRow row = sheet.createRow(j + 1);
			HSSFCell cell = row.createCell(0);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getName());

			cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getModel());

			cell = row.createCell(2);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getScope());

			cell = row.createCell(3);
			cell.setCellStyle(style);
			if (Common.isNotEmpty(stock.getGoodsStorage())) {
				cell.setCellValue(
						stock.getGoodsStorage().getShelfNumber() + "/" + stock.getGoodsStorage().getShelflevel());
			} else {
				cell.setCellValue("-/-");

			}
			cell = row.createCell(4);
			cell.setCellStyle(style);
			if (Common.isNotEmpty(stock.getUnit())) {
				cell.setCellValue(stock.getUnit().getName());
			} else {
				cell.setCellValue("");
			}

			cell = row.createCell(5);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getInventory());
			j++;
		}

	}

	/**
	 * 设置title
	 * 
	 * @return
	 */
	public List<String> title() {
		List<String> list = new ArrayList<>();
		list.add("设备名称");
		list.add("品名型号");
		list.add("使用范围");
		list.add("货架号/层");
		list.add("计量单位 ");
		list.add("当前库存");
		return list;
	}

	@Override
	@SystemServiceLog(description="查询低库存量信息")
	public List<Stock> findLowStock(int num) {

		Query query = new Query();
		if (num >= 0) {
			query.addCriteria(Criteria.where("inventory").lte(num));
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.ASC, "inventory")));
			List<Stock> list = this.find(query, Stock.class);
			return list;
		}

		return null;
	}

}
