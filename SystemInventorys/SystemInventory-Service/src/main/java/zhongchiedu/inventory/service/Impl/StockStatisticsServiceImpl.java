package zhongchiedu.inventory.service.Impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.bson.types.ObjectId;
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
import zhongchiedu.common.utils.Contents;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.Impl.UserServiceImpl;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.StockStatisticsService;

@Service
@Slf4j
public class StockStatisticsServiceImpl extends GeneralServiceImpl<StockStatistics> implements StockStatisticsService {

	private @Autowired StockServiceImpl stockService;

	private @Autowired UserServiceImpl userServiceService;

	@Override
	public Pagination<StockStatistics> findpagination(Integer pageNo, Integer pageSize, String search, String start,
			String end, String type, String id) {

		// 分页查询数据
		Pagination<StockStatistics> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(id)) {
				query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(id)));
			}
			query = this.findbySearch(search, start, end, type, query);
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, StockStatistics.class);
			if (pagination == null)
				pagination = new Pagination<StockStatistics>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	public Query findbySearch(String search, String start, String end, String type, Query query) {

		Criteria ca = new Criteria();
		Criteria ca1 = new Criteria();
		Criteria ca2 = new Criteria();
		if (type.equals("in")) {
			query.addCriteria(Criteria.where("inOrOut").is(true));
		} else if (type.equals("out")) {
			query.addCriteria(Criteria.where("inOrOut").is(false));
		}

		if (Common.isNotEmpty(search)) {
			List<Object> stockId = this.findStocks(search);
			List<Object> userId = this.findUsers(search);
			ca1.orOperator(Criteria.where("stock.$id").in(stockId), Criteria.where("user.$id").in(userId),
					Criteria.where("name").regex(search));
		}

		if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
			ca2.orOperator(Criteria.where("storageTime").gte(start).lte(end),
					Criteria.where("depotTime").gte(start).lte(end));
		}
		query.addCriteria(ca.andOperator(ca1, ca2));

		query.addCriteria(Criteria.where("isDelete").is(false));

		return query;

	}

	/**
	 * 模糊匹配类目的Id
	 * 
	 * @param search
	 * @return
	 */
	public List<Object> findStocks(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();

		query.addCriteria(ca.orOperator(Criteria.where("name").regex(search), Criteria.where("model").regex(search)));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Stock> lists = this.stockService.find(query, Stock.class);
		for (Stock li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	public List<Stock> findStocksBySearch(String search) {
		Query query = new Query();
		Criteria ca = new Criteria();
		query.addCriteria(ca.orOperator(Criteria.where("name").regex(search), Criteria.where("model").regex(search)));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Stock> lists = this.stockService.find(query, Stock.class);
		return lists;
	}

	public List<Object> findUsers(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();

		query.addCriteria(Criteria.where("userName").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Stock> lists = this.stockService.find(query, Stock.class);
		for (Stock li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@Override
	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics, HttpSession session) {

		long num = stockStatistics.getNum();
		if (num <= 0) {
			return BasicDataResult.build(400, "操作的数据有误！", null);
		}
		String id = stockStatistics.getStock().getId();// 获取库存设备id
		Stock stock = this.stockService.findOneById(id, Stock.class);
		if (Common.isNotEmpty(stock)) {
			User user = (User) session.getAttribute(Contents.USER_SESSION);
			stockStatistics.setUser(user);
			stockStatistics.setRevoke(false);
			if (stockStatistics.isInOrOut()) {
				// true == 入库
				// 更新库存中的库存
				long newNum = this.updateStock(stock, num, true);
				stockStatistics.setStorageTime(Common.fromDateH());
				stockStatistics.setNewNum(newNum);
				lockInsert(stockStatistics);
				return BasicDataResult.build(200, "商品入库成功", stockStatistics);
			} else {
				// 出库
				long newNum = this.updateStock(stock, num, false);
				if (newNum == -1) {
					// 出货数量不够
					return BasicDataResult.build(400, "货物库存数量不足", null);
				}
				stockStatistics.setDepotTime(Common.fromDateH());
				stockStatistics.setNewNum(newNum);
				lockInsert(stockStatistics);
				return BasicDataResult.build(200, "商品出库成功", stockStatistics);
			}
		} else {
			// 未能找到库存的信息，反馈界面入库失败
			return BasicDataResult.build(400, "未能找到库存商品", null);
		}
	}

	Lock lock = new ReentrantLock();
	Lock lockinsert = new ReentrantLock();

	public void lockInsert(StockStatistics stockStatistics) {

		lockinsert.lock();
		try {
			this.insert(stockStatistics);
		} finally {
			lockinsert.unlock();
		}

	}

	public long updateStock(Stock stock, long num, boolean inOrOut) {
		lock.lock();
		long oldnum = stock.getInventory();
		long newnum = 0;
		try {
			if (inOrOut) {
				// 入库
				newnum = oldnum + num;
				stock.setInventory(newnum);
				this.stockService.save(stock);
				return newnum;
			} else {
				// 出库
				if ((oldnum - num) < 0) {
					return -1;
				}
				newnum = oldnum - num;
				stock.setInventory(newnum);
				this.stockService.save(stock);
				return newnum;
			}
		} finally {
			lock.unlock();
		}

	}

	@Override
	public BasicDataResult revoke(String id) {
		StockStatistics st = this.findOneById(id, StockStatistics.class);
		if (st.isRevoke()) {
			return BasicDataResult.build(400, "该信息已经撤销，不能重复撤销", null);

		}
		if (Common.isEmpty(st.getStock())) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}
		String stockId = st.getStock().getId();
		Stock stock = this.stockService.findOneById(stockId, Stock.class);
		if (Common.isEmpty(stock)) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}

		long newNum = 0L;
		if (Common.isNotEmpty(st.getStorageTime())) {
			// 撤销出库
			newNum = this.updateStock(stock, st.getNum(), false);
		} else {
			// 撤销入库
			newNum = this.updateStock(stock, st.getNum(), true);
		}
		if (newNum == -1) {
			// 出货数量不够
			return BasicDataResult.build(400, "货物库存数量不足,无法撤销入库", null);
		}
		StockStatistics stockStatistics = updateStockStatistics(st);
		if (Common.isNotEmpty(stockStatistics)) {
			return BasicDataResult.build(200, "撤销成功", stockStatistics);
		}
		return BasicDataResult.build(400, "撤销过程中出现未知异常", null);

	}

	public StockStatistics updateStockStatistics(StockStatistics stockStatistics) {
		lockinsert.lock();
		try {
			long oldnum = stockStatistics.getNum();// 入库，出库数量
			stockStatistics.setRevokeNum(oldnum);
			stockStatistics.setRevoke(true);
			this.save(stockStatistics);
			return stockStatistics;
		} finally {
			lockinsert.unlock();
		}

	}

	@Override
	public HSSFWorkbook export(String search, String start, String end, String type, String name) {

		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = null;
		List<String> sheetName = new ArrayList<>();
		if (type.equals("in")) {
			// 入库统计
			sheetName.add("入库统计");
		} else if (type.equals("out")) {
			// 出库统计
			sheetName.add("出库统计");
		} else {
			// 统计所有
			sheetName.add("入库统计");
			sheetName.add("出库统计");
		}
		for (String i : sheetName) {
			if (i.equals("出库统计")) {
				type = "out";
			} else {
				type = "in";
			}

			sheet = wb.createSheet(i);
			HSSFCellStyle style = createStyle(wb);
			List<String> title = this.title();
			this.createHead(sheet, title, style, start+" \t"+ end + i);
			this.createTitle(sheet, title, style);
			this.createStock(sheet, title, style, search, start, end, type);
			sheet.createFreezePane(2, 0, 2, 0);
			sheet.setDefaultColumnWidth(20);
			sheet.autoSizeColumn(1, true);
		}
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
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));
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
	public void createStock(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String search, String start,
			String end, String type) {

		int j = 1;

		List<Stock> listStock = this.findStocksBySearch(search);
		// 获取所有的库存
		List<StockStatistics> list = this.findStockStatistics(search, start, end, type);
		String msg="";
		if (type.equals("in")) {
			// 入库统计
			msg="入库:";
		} else if (type.equals("out")) {
			// 出库统计
			msg="出库:";
		}
		for (Stock stock : listStock) {
			// 获取所有的设备
			HSSFRow row = sheet.createRow(j + 1);
			HSSFCell cell = row.createCell(0);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getName());

			cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getModel());
			int l = 1;
			for (StockStatistics st : list) {
				if (stock.getId().equals(st.getStock().getId())) {
					cell = row.createCell(l+1);
					cell.setCellStyle(style);
					if (st.isInOrOut()) {
						cell.setCellValue(st.getStorageTime());
					} else {
						cell.setCellValue(st.getDepotTime());
					}
					cell = row.createCell(l+2);
					cell.setCellStyle(style);
					cell.setCellValue(msg+st.getNum());

					l= l+2;
				}
			}
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
		list.add("日期");
		list.add("数量");
		return list;
	}

	public List<StockStatistics> findStockStatistics(String search, String start, String end, String type) {
		Query query = new Query();
		query = this.findbySearch(search, start, end, type, query);
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		query.addCriteria(Criteria.where("revoke").is(false));
		List<StockStatistics> list = this.find(query, StockStatistics.class);
		return list;
	}

	@Override
	public List<StockStatistics> findAllByDate(String date, boolean inOrOut) {
		Query query = new Query();
		if(inOrOut){
			//true 查入库
			query.addCriteria(Criteria.where("storageTime").regex(date)).addCriteria(Criteria.where("inOrOut").is(inOrOut));
		}else{
			//false 查出库
			query.addCriteria(Criteria.where("depotTime").regex(date)).addCriteria(Criteria.where("inOrOut").is(inOrOut));
		}
		List<StockStatistics> list = this.find(query, StockStatistics.class);
		return list;
	}

}
