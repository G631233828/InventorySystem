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
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.ProjectStockStatistics;
import zhongchiedu.inventory.service.ProjectStockStatisticsService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class ProjectStockStatisticsServiceImpl extends GeneralServiceImpl<ProjectStockStatistics>
		implements ProjectStockStatisticsService {

	private @Autowired ProjectStockServiceImpl projectStockService;

	private @Autowired UserServiceImpl userServiceService;

	@Override
	@SystemServiceLog(description = "分页查询库存统计信息")
	public Pagination<ProjectStockStatistics> findpagination(Integer pageNo, Integer pageSize, String search,
			String start, String end, String type, String id, String searchArea) {

		// 分页查询数据
		Pagination<ProjectStockStatistics> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(searchArea)) {
				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			}

			if (Common.isNotEmpty(id)) {
				query.addCriteria(Criteria.where("projectStock.$id").is(new ObjectId(id)));
			}
			query = this.findbySearch(search.trim(), start, end, type, query);
			
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, ProjectStockStatistics.class);
			if (pagination == null)
				pagination = new Pagination<ProjectStockStatistics>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@SystemServiceLog(description = "条件查询库统计信息")
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
			
			List<Object> stockId = this.findProjectStocks(search);
			
			List<Object> userId = this.findUsers(search);
			ca1.orOperator(Criteria.where("projectStock.$id").in(stockId), Criteria.where("user.$id").in(userId),
					Criteria.where("name").regex(search), Criteria.where("personInCharge").regex(search),
					Criteria.where("projectName").regex(search), Criteria.where("customer").regex(search));
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
	@SystemServiceLog(description = "条件查询库统计信息")
	public List<Object> findProjectStocks(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();

		query.addCriteria(ca.orOperator(Criteria.where("projectName").regex(search),Criteria.where("name").regex(search), Criteria.where("model").regex(search)));
		
		 query.addCriteria(Criteria.where("isDelete").is(false)); query.with(new
		 Sort(new Order(Direction.DESC, "createTime")));
		
		List<ProjectStock> lists = this.projectStockService.find(query, ProjectStock.class);
		for (ProjectStock li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@SystemServiceLog(description = "条件查询库统计信息")
	public List<ProjectStock> findStocksBySearch(String search, String areaId) {
		Query query = new Query();
		if (Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		Criteria ca = new Criteria();
		query.addCriteria(ca.orOperator(Criteria.where("name").regex(search), Criteria.where("model").regex(search)));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<ProjectStock> lists = this.projectStockService.find(query, ProjectStock.class);
		return lists;
	}

	@SystemServiceLog(description = "条件查询库统计信息")
	public List<Object> findUsers(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();

		query.addCriteria(Criteria.where("userName").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<ProjectStock> lists = this.projectStockService.find(query, ProjectStock.class);
		for (ProjectStock li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@Override
	@SystemServiceLog(description = "库存出库入库")
	public  BasicDataResult inOrOutstockStatistics(ProjectStockStatistics stockStatistics, User user) {

		
		
		Integer num = stockStatistics.getNum();
		if (num <= 0) {
			return BasicDataResult.build(400, "操作的数据有误！", null);
		}
		String id = stockStatistics.getProjectStock().getId();// 获取库存设备id

		ProjectStock projectStock = this.projectStockService.findOneById(id, ProjectStock.class);
		if (Common.isNotEmpty(projectStock)) {
			stockStatistics.setUser(user);
			stockStatistics.setRevoke(false);
			stockStatistics.setArea(projectStock.getArea());
			if (stockStatistics.isInOrOut()) {
				// true == 入库
				// 更新库存中的库存
				Integer newNum = this.updateProjectStock(projectStock, num, true, false);
				stockStatistics.setStorageTime(Common.fromDateH());
				stockStatistics.setNewNum(newNum);
				stockStatistics.setActualPurchaseQuantity(projectStock.getActualPurchaseQuantity());
				stockStatistics.setTotalActualCost(projectStock.getTotalActualCost());
				lockInsert(stockStatistics);
				return BasicDataResult.build(200, "商品入库成功", stockStatistics);
			} else {
				// 出库
				Integer newNum = this.updateProjectStock(projectStock, num, false, false);
				if (newNum == -1) {
					// 出货数量不够
					return BasicDataResult.build(400, "货物库存数量不足", null);
				}
				stockStatistics.setDepotTime(Common.fromDateH());
				stockStatistics.setNewNum(newNum);
//				stockStatistics.setNum(projectStock.getNum());
				stockStatistics.setActualPurchaseQuantity(projectStock.getActualPurchaseQuantity());
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

	@SystemServiceLog(description = "库存出库入库执行insert")
	public void lockInsert(ProjectStockStatistics stockStatistics) {

		lockinsert.lock();
		try {
			System.out.println(lockinsert.toString());
			System.out.println(lockinsert.hashCode());
			this.insert(stockStatistics);
		} finally {
			lockinsert.unlock();
		}

	}

	@SystemServiceLog(description = "更新库存信息")
	public Integer updateProjectStock(ProjectStock projectStock, Integer num, boolean inOrOut, boolean revoke) {
		lock.lock();
		Integer oldnum = projectStock.getInventory();
		Integer newnum = 0;
		//实际库存量
		int actualpurchase =  projectStock.getActualPurchaseQuantity();
		try {
			if (inOrOut) {
				// 入库
				newnum = oldnum + num;
				projectStock.setInventory(newnum);
				projectStock.setIsDelete(false);
				
				
				projectStock.setActualPurchaseQuantity(actualpurchase + num);
				if (revoke) {
					// 出库数量去除
					Integer revokeNum = projectStock.getNum();
					if ((revokeNum - num) < 0) {
						return -1;
					}
					projectStock.setNum(revokeNum - num);
					projectStock.setActualPurchaseQuantity(actualpurchase);
				}
					projectStock.setTotalActualCost(projectStock.getRealCostUnitPrice()*projectStock.getActualPurchaseQuantity());
				

				this.projectStockService.save(projectStock);
				return newnum;
			} else {
				// 出库
				if ((oldnum - num) < 0) {
					return -1;
				}
				newnum = oldnum - num;
				projectStock.setInventory(newnum);
				projectStock.setIsDelete(false);
				if (revoke) {
					projectStock.setActualPurchaseQuantity(actualpurchase-num);
					projectStock.setNum(projectStock.getNum());
				}else {
					projectStock.setNum(projectStock.getNum() + num);
				}
				projectStock.setTotalActualCost(projectStock.getRealCostUnitPrice()*projectStock.getActualPurchaseQuantity());
				this.projectStockService.save(projectStock);
				return newnum;
			}
		} finally {
			lock.unlock();
		}

	}

	@Override
	@SystemServiceLog(description = "撤销库存信息")
	public BasicDataResult revoke(String id) {
		ProjectStockStatistics st = this.findOneById(id, ProjectStockStatistics.class);
		if (st.isRevoke()) {
			return BasicDataResult.build(400, "该信息已经撤销，不能重复撤销", null);
		}
		if (Common.isEmpty(st.getProjectStock())) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}
		String projectStockId = st.getProjectStock().getId();
		ProjectStock projectStock = this.projectStockService.findOneById(projectStockId, ProjectStock.class);
		if (Common.isEmpty(projectStock)) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}

		long newNum = 0L;
		if (Common.isNotEmpty(st.getStorageTime())) {
			// 撤销入库   :::撤销出库之后 原数据中库存量增加  实际采购不变
			newNum = this.updateProjectStock(projectStock, st.getNum(), false, true);
		} else {
			// 撤销出库  :::撤销人库之后 原数据中库存量减少  实际采购减少
			newNum = this.updateProjectStock(projectStock, st.getNum(), true, true);
		}
		if (newNum == -1) {
			// 出货数量不够
			return BasicDataResult.build(400, "货物库存数量不足,无法撤销入库", null);
		}
		ProjectStockStatistics stockStatistics = updateProjectStockStatistics(st);
		if (Common.isNotEmpty(stockStatistics)) {
			return BasicDataResult.build(200, "撤销成功", stockStatistics);
		}
		return BasicDataResult.build(400, "撤销过程中出现未知异常", null);

	}

	@SystemServiceLog(description = "撤销后更新库存信息")
	public ProjectStockStatistics updateProjectStockStatistics(ProjectStockStatistics stockStatistics) {
		lockinsert.lock();
		try {
			Integer oldnum = stockStatistics.getNum();// 入库，出库数量
			stockStatistics.setRevokeNum(oldnum);
			stockStatistics.setRevoke(true);
			this.save(stockStatistics);
			return stockStatistics;
		} finally {
			lockinsert.unlock();
		}

	}

	@Override
	@SystemServiceLog(description = "导出库存统计信息")
	public HSSFWorkbook export(String search, String start, String end, String type, String name, String areaId) {

		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = null;
		List<String> sheetName = new ArrayList<>();
		if (type.equals("in")) {
			// 入库统计
			sheetName.add("项目入库统计");
		} else if (type.equals("out")) {
			// 出库统计
			sheetName.add("项目出库统计");
		} else {
			// 统计所有
			sheetName.add("项目入库统计");
			sheetName.add("项目出库统计");
		}
		for (String i : sheetName) {
			if (i.equals("项目出库统计")) {
				type = "out";
			} else {
				type = "in";
			}

			sheet = wb.createSheet(i);
			HSSFCellStyle style = createStyle(wb);
			List<String> title = this.title();
			this.createHead(sheet, title, style, start + " \t" + end + i);
			this.createTitle(sheet, title, style);
			this.createStock(sheet, title, style, search, start, end, type, areaId);
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
	public void createStock(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String search, String start,
			String end, String type, String areaId) {

		int j = 1;

		List<ProjectStock> listStock = this.findStocksBySearch(search, areaId);
		// 获取所有的库存
		List<ProjectStockStatistics> list = this.findProjectStockStatistics(search, start, end, type);
		String msg = "";
		if (type.equals("in")) {
			// 入库统计
			msg = "入库:";
		} else if (type.equals("out")) {
			// 出库统计
			msg = "出库:";
		}
		for (ProjectStock stock : listStock) {
			// 获取所有的设备
			HSSFRow row = sheet.createRow(j + 1);
			HSSFCell cell = row.createCell(0);
			cell.setCellStyle(style);
			if(Common.isNotEmpty(stock.getArea())) {
			cell.setCellValue(stock.getArea().getName());
			}else {
				cell.setCellValue("");
			}
			cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getProjectName());

			cell = row.createCell(2);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getName());

			cell = row.createCell(3);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getModel());
			int l = 3;
			for (ProjectStockStatistics st : list) {
				if (stock.getId().equals(st.getProjectStock().getId())) {
					cell = row.createCell(l + 1);
					cell.setCellStyle(style);
					if (st.isInOrOut()) {
						cell.setCellValue(st.getStorageTime());
					} else {
						cell.setCellValue(st.getDepotTime());
					}

					cell = row.createCell(l + 2);
					cell.setCellStyle(style);
					cell.setCellValue(msg + st.getNum());

					if (type.equals("out")) {
						cell = row.createCell(l + 3);
						cell.setCellStyle(style);
						cell.setCellValue("负责人：" + st.getPersonInCharge());

						cell = row.createCell(l + 4);
						cell.setCellStyle(style);
						cell.setCellValue("项目：" + st.getProjectName());

						cell = row.createCell(l + 5);
						cell.setCellStyle(style);
						cell.setCellValue("客户：" + st.getCustomer());

						l = l + 5;
					} else {
						l = l + 2;
					}
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
		list.add("区域");
		list.add("项目名称");
		list.add("设备名称");
		list.add("品名型号");
		list.add("日期");
		list.add("数量");
		return list;
	}

	@SystemServiceLog(description = "根据条件查询库存统计-findProjectStockStatistics")
	public List<ProjectStockStatistics> findProjectStockStatistics(String search, String start, String end,
			String type) {
		Query query = new Query();
		query = this.findbySearch(search, start, end, type, query);
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		query.addCriteria(Criteria.where("revoke").is(false));
		List<ProjectStockStatistics> list = this.find(query, ProjectStockStatistics.class);
		return list;
	}

	@Override
	@SystemServiceLog(description = "根据条件查询库存统计-findAllByDate")
	public List<ProjectStockStatistics> findAllByDate(String date, boolean inOrOut) {
		Query query = new Query();
		if (inOrOut) {
			// true 查入库
			query.addCriteria(Criteria.where("storageTime").regex(date))
					.addCriteria(Criteria.where("inOrOut").is(inOrOut)).addCriteria(Criteria.where("revoke").is(false));
		} else {
			// false 查出库
			query.addCriteria(Criteria.where("depotTime").regex(date))
					.addCriteria(Criteria.where("inOrOut").is(inOrOut)).addCriteria(Criteria.where("revoke").is(false));
		}
		List<ProjectStockStatistics> list = this.find(query, ProjectStockStatistics.class);
		return list;
	}

}
