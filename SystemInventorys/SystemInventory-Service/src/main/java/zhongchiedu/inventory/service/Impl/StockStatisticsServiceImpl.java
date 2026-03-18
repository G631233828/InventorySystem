package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import cn.afterturn.easypoi.entity.ImageEntity;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.MatrixToImageWriter;
import zhongchiedu.common.utils.PinyinTool;
import zhongchiedu.common.utils.WordUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.MultiMediaService;
import zhongchiedu.general.service.Impl.UserServiceImpl;
import zhongchiedu.inventory.pojo.MonthEndStatistics;
import zhongchiedu.inventory.pojo.NewCustomer;
import zhongchiedu.inventory.pojo.PickUpApplication;
import zhongchiedu.inventory.pojo.Pname;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.QrCode;
import zhongchiedu.inventory.pojo.RequestBo;
import zhongchiedu.inventory.pojo.Sign;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.MonthEndStatisticsService;
import zhongchiedu.inventory.service.PickUpApplicationService;
import zhongchiedu.inventory.service.PreStockService;
import zhongchiedu.inventory.service.QrCodeService;
import zhongchiedu.inventory.service.SignService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.inventory.service.SupplierService;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl.TimeRangeType;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class StockStatisticsServiceImpl extends GeneralServiceImpl<StockStatistics> implements StockStatisticsService {

	@Lazy
	private @Autowired StockServiceImpl stockService;

	private @Autowired UserServiceImpl userServiceService;

	@Autowired
	private QrCodeService qrCodeServce;
	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private MultiMediaService multiMediaService;

	@Autowired
	private SignService signService;

	@Autowired
	private SupplierService supplierService;

	@Autowired
	private PreStockService preStockService;

	@Autowired
	private NewCustomerServiceImpl newCustomerService;

	@Autowired
	private PickUpApplicationService pickUpApplicationService;

	@Autowired
	@Lazy
	private MonthEndStatisticsService monthEndStatisticsService;

	@Value("${qrcode.weburl}")
	private String weburl;
	@Value("${qrcode.height}")
	private int height;
	@Value("${qrcode.width}")
	private int width;
	@Value("${upload.savedir}")
	private String dir;
	@Value("${qrcode.qrcodepath}")
	private String qrcodepath;
	@Value("${qrcode.format}")
	private String format;

	@Override
	@SystemServiceLog(description = "分页查询库存统计信息")
	public Pagination<StockStatistics> findpagination(Integer pageNo, Integer pageSize, String search, String start,
			String end, String type, String id, String searchArea, String searchAgent, String userId, String revoke,
			String confirm, String ssC) {

		// 分页查询数据
		Pagination<StockStatistics> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(id)) {
				query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(id)));
			}
			if (Common.isNotEmpty(searchAgent)) {
				query = query.addCriteria(Criteria.where("agent").is(Boolean.valueOf(searchAgent)));
			}
			if (Common.isNotEmpty(confirm)) {
				query = query.addCriteria(Criteria.where("confirm").is(Boolean.valueOf(confirm)));
			}
			if (Common.isNotEmpty(userId)) {
				query = query.addCriteria(Criteria.where("financeUser.$id").is(new ObjectId(userId)));
			}
			if (Common.isNotEmpty(revoke)) {
				if (revoke.equals("1")) {
					query.addCriteria(Criteria.where("revoke").is(true));
				} else if (revoke.equals("2")) {
					query.addCriteria(Criteria.where("revoke").is(false));
				}
			}
			query = this.findbySearch(search, start, end, type, query, searchArea, ssC);
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

	public Pagination<StockStatistics> findpagination(Integer pageNo, Integer pageSize, RequestBo requestBo) {
		// 分页查询数据
		Pagination<StockStatistics> pagination = null;
		try {
			Query query = newQueryByRequestBo(requestBo);
			if (Common.isNotEmpty(requestBo.getRevoke())) {
				if (requestBo.getRevoke().equals("1")) {
					query.addCriteria(Criteria.where("revoke").is(true));
				} else if (requestBo.getRevoke().equals("2")) {
					query.addCriteria(Criteria.where("revoke").is(false));
				}
			}
			if (Common.isNotEmpty(requestBo.getMysign())) {
				if (requestBo.getMysign().equals("1")) {
					query.addCriteria(Criteria.where("mysign").exists(true));
				} else if (requestBo.getMysign().equals("2")) {
					query.addCriteria(Criteria.where("mysign").exists(false));
				}
			}
			if (Common.isNotEmpty(requestBo.getOthersign())) {
				if (requestBo.getOthersign().equals("1")) {
					query.addCriteria(Criteria.where("othersign").exists(true));
				} else if (requestBo.getOthersign().equals("2")) {
					query.addCriteria(Criteria.where("othersign").exists(false));
				}
			}
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, StockStatistics.class);
			if (pagination == null)
				pagination = new Pagination<StockStatistics>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;

	}

	@SystemServiceLog(description = "条件查询库统计信息")
	public Query findbySearch(String search, String start, String end, String type, Query query, String areaId,
			String ssC) {
		Criteria ca = new Criteria();
		Criteria ca1 = new Criteria();
		Criteria ca2 = new Criteria();
		Criteria ca3 = new Criteria();
		end = end + " 23:59:59";
		if (type.equals("in")) {
			query.addCriteria(Criteria.where("inOrOut").is(true));
		} else if (type.equals("out")) {
			query.addCriteria(Criteria.where("inOrOut").is(false));
		}

		if (Common.isNotEmpty(areaId)) {
			query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		List<Object> stockIdByssC = null;
		if (Common.isNotEmpty(ssC)) {
			stockIdByssC = findStocksByssCId(ssC);
			ca3.orOperator(Criteria.where("stock.$id").in(stockIdByssC));
		}
		if (Common.isNotEmpty(search)) {
			List<Object> stockId = this.findStocks(search);
			List<Object> userId = this.findUsers(search);
			ca1.orOperator(Criteria.where("stock.$id").in(stockId), Criteria.where("user.$id").in(userId),
					Criteria.where("name").regex(search), Criteria.where("personInCharge").regex(search),
					Criteria.where("projectName").regex(search), Criteria.where("customer").regex(search),
					Criteria.where("sailesInvoiceNo").regex(search), Criteria.where("inprice").regex(search),
					Criteria.where("purchaseInvoiceNo").regex(search), Criteria.where("receiptNo").regex(search),
					Criteria.where("newItemNo").regex(search), Criteria.where("outboundOrder").regex(search),
					Criteria.where("accepter").regex(search), Criteria.where("paymentOrderNo").regex(search));
		}

		if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
			ca2.orOperator(Criteria.where("storageTime").gte(start).lte(end),
					Criteria.where("depotTime").gte(start).lte(end)
//					,
//					Criteria.where("editFinanceTime").gte(start).lte(end),
//					Criteria.where("sailesInvoiceDate").gte(start).lte(end)
			);
		}
		query.addCriteria(ca.andOperator(ca1, ca2, ca3));

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
	public List<Object> findStocks(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		List<Object> findSupplierIds = this.supplierService.findSupplierIds(search);
		Criteria ca = new Criteria();
		ca.orOperator(Criteria.where("entryName").regex(search), Criteria.where("itemNo").regex(search),
				Criteria.where("name").regex(search, "i"), Criteria.where("supplier.$id").in(findSupplierIds),
				Criteria.where("projectLeader").regex(search),
				Criteria.where("model").regex(Common.escapeExprSpecialWord(search), "i"));
//				Criteria.where("model").regex("^" +search.replace("*",".*") + "$", "i"));
		query.addCriteria(ca);
//		query.addCriteria(Criteria.where("isDelete").is(false));  添加此条件，则被删除的库存无法在库存统计中显示
		List<Stock> lists = this.stockService.find(query, Stock.class);
		for (Stock li : lists) {
//			System.out.println(li.getId()+":"+li.getName());
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@SystemServiceLog(description = "根据分类的id查询stock")
	public List<Object> findStocksByssCId(String ssC) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query = query.addCriteria(Criteria.where("systemClassification.$id").is(new ObjectId(ssC)));
		List<Stock> lists = this.stockService.find(query, Stock.class);
		for (Stock li : lists) {
//			System.out.println(li.getId()+":"+li.getName());
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@SystemServiceLog(description = "条件查询库统计信息")
	public List<Stock> findStocksBySearch(String search, String areaId, String searchAgent) {
		Query query = new Query();
		if (Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}

		if (Common.isNotEmpty(searchAgent)) {
			query = query.addCriteria(Criteria.where("agent").is(Boolean.valueOf(searchAgent)));
		}
		if (Common.isNotEmpty(search)) {
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(Criteria.where("name").regex(search),
					Criteria.where("model").regex(Common.escapeExprSpecialWord(search))));

		}

//		query.addCriteria(Criteria.where("isDelete").is(false)); 被删除的库存也能在库存统计的报表中显示
		List<Stock> lists = this.stockService.find(query, Stock.class);
		return lists;
	}

	@SystemServiceLog(description = "条件查询库统计信息")
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
	@SystemServiceLog(description = "库存出库入库")
	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics, User user) {
		Double num = 0.0;
		if (stockStatistics.isRevoke()) {
			num = stockStatistics.getNum();// 获取到撤销数量
			// 如果撤销数量为0 说明是撤销全部
			if (num == 0) {
				num = stockStatistics.getNum();
			}
		} else {
			num = stockStatistics.getNum();
			if (num <= 0) {
				return BasicDataResult.build(400, "操作的数据有误！", null);
			}
		}
		// 对num进行小数点四舍五入
		num = Math.round(num * 100) / 100.0;

		String id = stockStatistics.getStock().getId();// 获取库存设备id
		Stock stock = this.stockService.findOneById(id, Stock.class);

		if (stock != null) {

			Double ycknum = 0.0;
			Double acnum = 0.0;
			if (!stockStatistics.isPreStock() && !stockStatistics.isInOrOut()) {
				if (!stockStatistics.isYck()) {
					List<PickUpApplication> pickUpApplication = this.pickUpApplicationService
							.findPickUpApplicationsByStockId(stock.getId());

//					ycknum = pickUpApplication.stream().map(PickUpApplication::getEstimatedIssueQuantity)
//							.reduce((double) 0, Double::sum);
//
//					acnum = pickUpApplication.stream().map(PickUpApplication::getActualIssueQuantity).reduce((double) 0,
//							Double::sum);

					ycknum = pickUpApplication.stream()
							// 过滤集合中的null对象
							.filter(Objects::nonNull)
							// 映射为数量，并过滤null结果
							.map(PickUpApplication::getEstimatedIssueQuantity).filter(Objects::nonNull)
							// 累加（初始值0.0，避免空流时返回null）
							.reduce(0.0, Double::sum);

					acnum = pickUpApplication.stream().filter(Objects::nonNull)
							.map(PickUpApplication::getActualIssueQuantity).filter(Objects::nonNull)
							.reduce(0.0, Double::sum);

					ycknum = Math.round((ycknum - acnum) * 100) / 100.0;

					if (stock.getInventory() - ycknum - num < 0) {
						// 出货数量不够
						return BasicDataResult.build(400, "货物库存数量不足", null);
					}
				}

			}

//			stock.setDescription(stockStatistics.getDescription());
			stockStatistics.setUser(user);
			stockStatistics.setRevoke(stockStatistics.isRevoke());
			stockStatistics.setArea(stock.getArea());
			stockStatistics.setAgent(stock.isAgent());
			if (stockStatistics.isInOrOut()) {
				// true == 入库
				// 更新库存中的库存
				Double newNum = this.updateStock(stock, num, true);
				stockStatistics.setStorageTime(Common.fromDateH());
				stockStatistics.setNewNum(newNum);
				stockStatistics.setRemainingNum(newNum - ycknum);
				lockInsert(stockStatistics);

				return BasicDataResult.build(200, "商品入库成功", stockStatistics);
			} else {

				// 出库
				Double newNum = this.updateStock(stock, num, false);
				if (newNum == -1.0) {
					// 出货数量不够
					return BasicDataResult.build(400, "货物库存数量不足", null);
				}
				
				String dj =stock.getPrice();
				
				System.out.println(stock.getPrice());
				System.out.println(stock.getPrice() == null);
				System.out.println(stock.getPrice() == "");
				if(Common.isEmpty(stock.getPrice())) {
					 try {
	                     BigDecimal avgPrice = this.calculateAveragePriceByStockId(stock.getId(), TimeRangeType.CURRENT_MONTH,"");
	                     if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
	                         dj = String.valueOf(avgPrice.doubleValue());
	                     }
	                 } catch (Exception e) {
	                     // 调用失败则设为0.0，避免导出中断
	                     dj = "0.0";
	                 }
				}
				
				stockStatistics.setPrice(dj);
				stockStatistics.setRemainingNum(newNum - ycknum);
				stockStatistics.setDepotTime(Common.fromDateH());
				stockStatistics.setNewNum(newNum);
				lockInsert(stockStatistics);
				// 刷新redis中的projectName
				this.redisTemplate.delete("projectNames");
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
	public void lockInsert(StockStatistics stockStatistics) {

		lockinsert.lock();
		try {
			stockStatistics.setNum(Math.round(stockStatistics.getNum() * 100) / 100.0);
			this.insert(stockStatistics);
		} finally {
			lockinsert.unlock();
		}

	}

	@SystemServiceLog(description = "更新库存信息")
	public Double updateStock(Stock stock, Double num, boolean inOrOut) {
		lock.lock();
		Double oldnum = stock.getInventory();
		Double newnum = 0.0;
		try {
			if (inOrOut) {
				// 入库
				newnum = Math.round((oldnum + num) * 100) / 100.0;
				stock.setInventory(newnum);
				stock.setIsDelete(false);
				stock.setUpdateTime(new Date());
				this.stockService.save(stock);
				return newnum;
			} else {
				// 出库
				if ((oldnum - num) < 0) {
					return -1.0;
				}
				newnum = Math.round((oldnum - num) * 100) / 100.0;
				stock.setInventory(newnum);
				stock.setIsDelete(false);
				this.stockService.save(stock);
				return newnum;
			}
		} finally {
			lock.unlock();
		}

	}

	@SystemServiceLog(description = "撤销预入库信息")
	public Double updatePreStock(Stock stock, Double num, boolean inOrOut, StockStatistics st) {
		lock.lock();
		Double oldnum = stock.getInventory();
		Double newnum = 0.0;
		try {
			// 撤销入库
			if ((oldnum - num) < 0) {
				return -1.0;
			}
			if (st.isPreStock()) {
				PreStock preStock = preStockService.findOneById(st.getPreStockId(), PreStock.class);
				Double renum = preStock.getActualReceiptQuantity() - num;
				preStock.setActualReceiptQuantity(renum);
				preStock.setStatus(1);
				this.preStockService.save(preStock);
			}
			newnum = oldnum - num;
			stock.setInventory(newnum);
			stock.setIsDelete(false);
			this.stockService.save(stock);
			return newnum;

		} finally {
			lock.unlock();
		}
//
	}

	@Override
	@SystemServiceLog(description = "撤销库存信息")
	public BasicDataResult revoke(String id, Double num, User user) {
		StockStatistics st = this.findOneById(id, StockStatistics.class);
		if (st.isRevoke()) {
			return BasicDataResult.build(400, "该信息已经撤销，不能重复撤销", null);

		}
		if (st.getStock() == null) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}
		if (num <= 0.0) {
			num = st.getNum();
		}

		if (num > st.getNum()) {
			return BasicDataResult.build(400, "撤销出库数量不能大于出库数量", null);
		}

		String stockId = st.getStock().getId();
		Stock stock = this.stockService.findOneById(stockId, Stock.class);
		if (stock == null) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}

		Double newNum = 0.0;
		if (Common.isNotEmpty(st.getStorageTime())) {
			// 撤销入库
			st.setDescription("<label style=\"color:red\">来源：撤销入库</label>");
			st.setByRevoke(true);
			newNum = this.updatePreStock(stock, st.getNum(), false, st);
			if (newNum == -1) {
				// 出货数量不够
				return BasicDataResult.build(400, "货物库存数量不足,无法撤销入库", null);
			}
			// 更新统计
			st.setRevoke(true);
			st.setRevokeNum(st.getRevokeNum() == null ? 0.0 : st.getRevokeNum() + num);

			StockStatistics stockStatistics = updateStockStatistics(st);

			if (stockStatistics != null) {
//				StockStatistics revoke = new StockStatistics();
//				revoke.setRevokeNum(stockStatistics.getRevokeNum());
				//
				return BasicDataResult.build(200, "撤销成功", stockStatistics);
			}

		} else {
			// 撤销出库
			// newNum = this.updateStock(stock, st.getNum(), true);
			// 执行入库统计记录
			StockStatistics getst = new StockStatistics();
			getst.setInOrOut(true);
			getst.setRevoke(true);
			getst.setNum(num);
			getst.setStock(st.getStock());
			getst.setYck(true);
			getst.setByRevoke(true);
			getst.setDescription("<label style=\"color:red\">来源：撤销出库</label>");
			BasicDataResult inOrOutstockStatistics = this.inOrOutstockStatistics(getst, user);
			StockStatistics newst = (StockStatistics) inOrOutstockStatistics.getData();
			// 更新统计
			st.setRevoke((st.getNum() - num) <= 0);
			st.setNum(st.getNum() - num);
			st.setRevokeNum(num);
//			st.setDepotTime(Common.fromDateH());
			st.setNewNum(newst.getNewNum());

			StockStatistics stockStatistics = updateStockStatistics(st);

			if (stockStatistics != null) {
//				StockStatistics revoke = new StockStatistics();
//				revoke.setRevokeNum(stockStatistics.getRevokeNum());
				//
				return BasicDataResult.build(200, "撤销成功", stockStatistics);
			}
		}

//		
//		if (stockStatistics != null) {
//			StockStatistics revoke = new StockStatistics();
//			revoke.setRevokeNum(stockStatistics.getRevokeNum());
//
//			return BasicDataResult.build(200, "撤销成功", revoke);
//		}
		return BasicDataResult.build(400, "撤销过程中出现未知异常", null);

	}

	@Override
	@SystemServiceLog(description = "核对信息")
	public BasicDataResult confirm(String id) {
		StockStatistics st = this.findOneById(id, StockStatistics.class);
		if (st.getConfirm()) {
			return BasicDataResult.build(400, "该信息已经核对，不需要核对", null);
		}
		st.setConfirm(true);
		this.save(st);
		return BasicDataResult.build(200, "已核对", st);
	}

	@SystemServiceLog(description = "撤销后更新库存信息")
	public StockStatistics updateStockStatistics(StockStatistics stockStatistics) {
		lockinsert.lock();
		try {
//			long oldnum = stockStatistics.getNum();// 入库，出库数量
//			stockStatistics.setRevokeNum(oldnum);
//			stockStatistics.setRevoke(true);
			this.save(stockStatistics);
			return stockStatistics;
		} finally {
			lockinsert.unlock();
		}

	}

	@Override
	@SystemServiceLog(description = "导出库存统计信息")
	public Workbook newExport(HttpServletRequest request, RequestBo requestBo) {
	    // ========== 新增：日期判断 + 历史月度统计数据查询 ==========
	    String end = "";
	    boolean isCurrentMonth = false; // 标记是否为当月查询
	    Map<String, MonthEndStatistics> stockIdToHistoryStats = new HashMap<>();

	    // 1. 处理时间范围，判断是否为当月
	    if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
	        end = requestBo.getEnd() + " 23:59:59";
	        // 判断当前查询的结束时间是否为当月
	        isCurrentMonth = Common.isDateTimeInCurrentMonth(end);

	        // 2. 查询历史月度统计数据（用于历史月份单价获取）
	        String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(requestBo.getStart());
	        List<MonthEndStatistics> findMonthEndStatisticsByDate = this.monthEndStatisticsService
	                .findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
	        stockIdToHistoryStats = listMonthEndStatisticsToMap(findMonthEndStatisticsByDate);
	    }
	    // ========== 原有基础数据查询逻辑 ==========
	    Query querys = new Query();
	    querys = this.stockService.findByRequestBo(requestBo, querys);
	    List<Stock> listStock = this.stockService.find(querys, Stock.class);

	    // 获取所有的库存统计数据
	    Query query = newQueryByRequestBo(requestBo);
	    query.addCriteria(Criteria.where("revoke").is(false));
	    List<StockStatistics> list = this.find(query, StockStatistics.class);

	    List<Map<String, Object>> inlist = new ArrayList<>();
	    List<Map<String, Object>> outlist = new ArrayList<>();

	    for (Stock stock : listStock) {
	        // 获取所有的设备
	        for (StockStatistics st : list) {
	            if (st.getStock() != null && stock.getId().equals(st.getStock().getId())) {
	            	
	            	  Double dj = 0.0;
	                    // 增加非数字转换异常防护
	                    if (Common.isNotEmpty(st.getPrice())) {
	                        try {
	                            dj = Double.parseDouble(st.getPrice());
	                        } catch (NumberFormatException e) {
	                            dj = 0.0; // 非数字则设为0.0
	                        }
	                    }
	                if (st.isInOrOut()) {
	                    // ========== 入库逻辑：保留原有逻辑，仅增加数据转换防护 ==========
	                    Double num = Common.isNotEmpty(st.getNum()) ? st.getNum() : 0.0;
	                 

	                    // 入库统计（原有字段结构不变）
	                    Map<String, Object> in = new HashMap<>();
	                    in.put("t1", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
	                    in.put("t2", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
	                    in.put("t3", Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
	                    in.put("t4", st.getStorageTime());
	                    in.put("t5", num);
	                    in.put("t6", dj);
	                    in.put("t7", Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
	                    in.put("t8", Common.isEmpty(st.getStock().getItemNo()) ? "" : st.getStock().getItemNo());
	                    in.put("t9", Common.isEmpty(st.getStock().getSupplier()) ? "" : st.getStock().getSupplier().getName());
	                    in.put("t10", num * dj);
	                    in.put("t11", Common.isEmpty(st.getPname()) ? "" : st.getPname().getName());
	                    in.put("t12", st.getDescription());
	                    in.put("t13", Common.isEmpty(st.getUser()) ? "" : st.getUser().getUserName());
	                    inlist.add(in);
	                } else {
	                    // ========== 出库逻辑：核心修改 - 分当月/历史月份取单价 ==========
	                    Double num = Common.isNotEmpty(st.getNum()) ? st.getNum() : 0.0;
	                   // Double dj = 0.0; // 最终出库单价
	                    String stockId = stock.getId();

//	                    // 分场景获取单价
//	                    if (isCurrentMonth) {
//	                        // 当月：调用calculateAveragePriceByStockId获取本月平均单价（BigDecimal转Double）
//	                        try {
//	                            BigDecimal avgPrice = this.calculateAveragePriceByStockId(stockId, TimeRangeType.CURRENT_MONTH);
//	                            if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
//	                                dj = avgPrice.doubleValue();
//	                            }
//	                        } catch (Exception e) {
//	                            // 调用失败则设为0.0，避免导出中断
//	                            dj = 0.0;
//	                        }
//	                    } else {
//	                        // 历史月份：从MonthEndStatistics获取单价
//	                        MonthEndStatistics historyStat = stockIdToHistoryStats.get(stockId);
//	                        if (historyStat != null && Common.isNotEmpty(historyStat.getPrice())) {
//	                            try {
//	                                // 历史单价转Double，非数字则设为0.0
//	                                dj = Double.parseDouble(historyStat.getPrice());
//	                            } catch (NumberFormatException e) {
//	                                dj = 0.0;
//	                            }
//	                        }
//	                    }

	                    // 出库统计（仅调整dj取值，原有字段结构不变）
	                    Map<String, Object> out = new HashMap<>();
	                    out.put("ta", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
	                    out.put("b", Common.isEmpty(st.getPname()) ? "" : st.getPname().getName());
	                    out.put("t1", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
	                    out.put("t2", Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
	                    // t3字段：展示最终的单价字符串（保留原有格式）
	                    out.put("t3", Common.isEmpty(String.valueOf(dj)) ? "0.0" : String.valueOf(dj));
	                    out.put("t4", st.getDepotTime());
	                    out.put("t5", num);
	                    out.put("t6", Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
	                    out.put("t7", num * dj); // 总金额使用新的dj计算
	                    out.put("t8", Common.isEmpty(st.getNewCustomer()) ? "" : st.getNewCustomer().getName());
	                    out.put("t9", Common.isEmpty(st.getStock().getSupplier()) ? "" : st.getStock().getSupplier().getName());
	                    // 增加st.getUser()空值防护，避免NPE
	                    out.put("t10", Common.isEmpty(st.getUser()) ? "" : (Common.isEmpty(st.getUser().getUserName()) ? "" : st.getUser().getUserName()));
	                    // 增加st.getPname()空值防护，避免NPE
	                    out.put("t11", Common.isEmpty(st.getPname()) ? "" : (Common.isEmpty(st.getPname().getPm()) ? "" : st.getPname().getPm()));
	                    out.put("t12", st.getAccepter());
	                    out.put("t13", st.getDescription());
	                    out.put("t14", Common.isEmpty(st.getSign()) ? "未签名" : "已签名");
	                    out.put("t15", Common.isEmpty(st.getOthersign()) ? "未签名" : "已签名");
	                    outlist.add(out);
	                }
	            }
	        }
	    }

	    // ========== 原有导出逻辑 ==========
	    Map<String, Object> dataMap = new HashMap<>();
	    dataMap.put("inlist", inlist);
	    dataMap.put("outlist", outlist);

	    String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
	    String fileName = "库存统计导出模板.xlsx";
	    TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
	    Workbook doc = null;

	    try {
	        doc = ExcelExportUtil.exportExcel(params, dataMap);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return doc;
	}
//	public Workbook newExport(HttpServletRequest request, RequestBo requestBo) {
//
////		List<Stock> listStock = this.findStocksBySearch(search, areaId, searchAgent);
//		// 获取所有的库存
////		List<StockStatistics> list = this.findStockStatistics(search, start, end, type, areaId, searchAgent);
//
//		Query querys = new Query();
//		querys = this.stockService.findByRequestBo(requestBo, querys);
////		querys.addCriteria(Criteria.where("isDelete").is(false));
//		List<Stock> listStock = this.stockService.find(querys, Stock.class);
//
//		// 获取所有的库存统计数据
//		Query query = newQueryByRequestBo(requestBo);
//		query.addCriteria(Criteria.where("revoke").is(false));
//		List<StockStatistics> list = this.find(query, StockStatistics.class);
//
//		List<Map<String, Object>> inlist = new ArrayList<>();
//		List<Map<String, Object>> outlist = new ArrayList<>();
//
//		for (Stock stock : listStock) {
//			// 获取所有的设备
//			for (StockStatistics st : list) {
//
//				if (st.getStock() != null) {
//					if (stock.getId().equals(st.getStock().getId())) {
//						if (st.isInOrOut()) {
//
//							Double num = Common.isNotEmpty(st.getNum()) ? st.getNum() : 0.0;
//							Double dj = Common.isNotEmpty(st.getPrice())
//									? Double.parseDouble(st.getPrice())
//									: 0.0;
//
//							// 入库统计
//							Map<String, Object> in = new HashMap<>();
//							in.put("t1", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
//							in.put("t2", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
//							in.put("t3", Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
//							in.put("t4", st.getStorageTime());
//							in.put("t5", num);
//							in.put("t6", dj);
//							in.put("t7",
//									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
//							in.put("t8", Common.isEmpty(st.getStock().getItemNo()) ? "" : st.getStock().getItemNo());
//							in.put("t9", Common.isEmpty(st.getStock().getSupplier()) ? ""
//									: st.getStock().getSupplier().getName());
//							in.put("t10", num * dj);
//							in.put("t11", Common.isEmpty(st.getPname()) ? "" : st.getPname().getName());
//							in.put("t12", st.getDescription());
//							in.put("t13", Common.isEmpty(st.getUser()) ? "" : st.getUser().getUserName());
//
////							in.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
////							in.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
////							in.put("stockName", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
////							in.put("modelName",
////									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
////							in.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
////							in.put("inprice", Common.isEmpty(st.getInprice()) ? "" : st.getInprice());
////							in.put("description", Common.isEmpty(st.getDescription()) ? "" : st.getDescription());
////							in.put("unit",
////									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
////							in.put("depotTime", st.getStorageTime());
////							in.put("num", st.getNum());
////							in.put("purchaseInvoiceNo",
////									Common.isEmpty(st.getPurchaseInvoiceNo()) ? "" : st.getPurchaseInvoiceNo());
////
////							in.put("newItemNo", Common.isEmpty(st.getNewItemNo()) ? "" : st.getNewItemNo());
////							in.put("paymentOrderNo",
////									Common.isEmpty(st.getPaymentOrderNo()) ? "" : st.getPaymentOrderNo());
////							in.put("supplier", Common.isEmpty(st.getStock().getSupplier()) ? ""
////									: st.getStock().getSupplier().getName());
////							in.put("purchaseInvoiceDate",
////									Common.isEmpty(st.getPurchaseInvoiceDate()) ? "" : st.getPurchaseInvoiceDate());
//							inlist.add(in);
//						} else {
//							Map<String, Object> out = new HashMap<>();
//							Double num = Common.isNotEmpty(st.getNum()) ? st.getNum() : 0.0;
//							Double dj = Common.isNotEmpty(st.getStock().getPrice())
//									? Double.parseDouble(st.getStock().getPrice())
//									: 0.0;
//							out.put("ta", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
//							out.put("b", Common.isEmpty(st.getPname()) ? "" : st.getPname().getName());
//							out.put("t1", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
//							out.put("t2", Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
//							out.put("t3", Common.isEmpty(st.getPrice()) ? "0.0" : st.getPrice());
//							out.put("t4", st.getDepotTime());
//							out.put("t5", num);
//							out.put("t6",
//									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
//							out.put("t7", num * dj);
//							out.put("t8", Common.isEmpty(st.getNewCustomer()) ? "" : st.getNewCustomer().getName());
//							out.put("t9", Common.isEmpty(st.getStock().getSupplier()) ? ""
//									: st.getStock().getSupplier().getName());
//							out.put("t10",
//									Common.isEmpty(st.getUser().getUserName()) ? "" : st.getUser().getUserName());
//							out.put("t11", Common.isEmpty(st.getPname().getPm()) ? "" : st.getPname().getPm());
//							;
//							out.put("t12", st.getAccepter());
//							out.put("t13", st.getDescription());
//							out.put("t14", Common.isEmpty(st.getSign()) ? "未签名" : "已签名");
//							out.put("t15", Common.isEmpty(st.getOthersign()) ? "未签名" : "已签名");
////							out.put("itemNo", Common.isEmpty(stock.getItemNo()) ? "" : stock.getItemNo());
////							out.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
////							out.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
////							out.put("stockName",
////									Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
////							out.put("modelName",
////									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
////							out.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
////							out.put("unit",
////									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
////							out.put("depotTime", st.getDepotTime());
////							out.put("num", st.getNum());
////							out.put("sailesInvoiceNo",
////									Common.isEmpty(st.getSailesInvoiceNo()) ? "" : st.getSailesInvoiceNo());
////							out.put("sailPrice", Common.isEmpty(st.getSailPrice()) ? "" : st.getSailPrice());
////							out.put("sailesInvoiceDate",
////									Common.isEmpty(st.getSailesInvoiceDate()) ? "" : st.getSailesInvoiceDate());
////							out.put("receiptNo", Common.isEmpty(st.getReceiptNo()) ? "" : st.getReceiptNo());
////							out.put("customer", Common.isEmpty(st.getCustomer()) ? "" : st.getCustomer());
////							out.put("purchaseInvoiceDate",
////									Common.isEmpty(st.getPurchaseInvoiceDate()) ? "" : st.getPurchaseInvoiceDate());
////							out.put("description", Common.isEmpty(st.getDescription()) ? "" : st.getDescription());
//							outlist.add(out);
//
//						}
//
//					}
//				}
//			}
//		}
//
//		Map<String, Object> dataMap = new HashMap<>();
//
//		dataMap.put("inlist", inlist);
//		dataMap.put("outlist", outlist);
//
//		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
//		String fileName = "库存统计导出模板.xlsx";
//		TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
//		Workbook doc = null;
//
//		try {
//			doc = ExcelExportUtil.exportExcel(params, dataMap);
////							WordUtil.exportWord(ctxPath+fileName, dataMap);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return doc;
//
//	}

	@Override
	@SystemServiceLog(description = "导出适配金蝶的报表")
	public Workbook toJD(HttpServletRequest request, RequestBo requestBo) {
		Query query = new Query();
		Criteria ca = new Criteria();
		List<Stock> listStock = null;
		if (!requestBo.isEmpty()) {
			Query querys = new Query();
			querys = this.stockService.findByRequestBo(requestBo, querys);
			listStock = this.stockService.find(querys, Stock.class);
			List<Object> stockids = listStock.stream().map(stock -> new ObjectId(stock.getId()))
					.collect(Collectors.toList());
			query = query.addCriteria(Criteria.where("stock.$id").in(stockids));
		}
		if (Common.isNotEmpty(requestBo.getId())) {
			ca.orOperator(Criteria.where("stock.$id").is(new ObjectId(requestBo.getId())));
		}
		if (Common.isNotEmpty(requestBo.getUserId())) {
			ca.orOperator(Criteria.where("financeUser.$id").is(new ObjectId(requestBo.getUserId())));
		}
		if (Common.isNotEmpty(requestBo.getItemNo())) {
			query = query.addCriteria(Criteria.where("newItemNo").regex(requestBo.getItemNo(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getPurchaseInvoiceNo())) {
			query = query.addCriteria(Criteria.where("purchaseInvoiceNo").regex(requestBo.getPurchaseInvoiceNo(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getPaymentOrderNo())) {
			query = query.addCriteria(Criteria.where("paymentOrderNo").regex(requestBo.getPaymentOrderNo(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getPurchaseInvoiceDate())) {
			query = query
					.addCriteria(Criteria.where("purchaseInvoiceDate").regex(requestBo.getPurchaseInvoiceDate(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getConfirm())) {
			query = query.addCriteria(Criteria.where("confirm").is(Boolean.valueOf(requestBo.getConfirm())));
		}
		if (Common.isNotEmpty(requestBo.getType())) {
			if (requestBo.getType().equals("in")) {
				query.addCriteria(Criteria.where("inOrOut").is(true));
			} else if (requestBo.getType().equals("out")) {
				query.addCriteria(Criteria.where("inOrOut").is(false));
			}
		}
		if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
			String end = requestBo.getEnd();
			end = end + " 23:59:59";
			Criteria ca1 = new Criteria();
			ca1.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
					Criteria.where("depotTime").gte(requestBo.getStart()).lte(end));
			ca.andOperator(ca1);
		}
		query.addCriteria(ca);
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		String end = requestBo.getEnd();
		query.addCriteria(Criteria.where("revoke").is(false));
		List<StockStatistics> list = this.find(query, StockStatistics.class);
		List<Map<String, Object>> inlist = new ArrayList<>();
		List<Map<String, Object>> outlist = new ArrayList<>();
		int inN = 0;
		int OutN = 0;
		for (Stock stock : listStock) {
			// 获取所有的设备
			for (StockStatistics st : list) {

				if (st.getStock() != null) {
					if (stock.getId().equals(st.getStock().getId())) {
						if (st.isInOrOut()) {
							// 入库统计
							Map<String, Object> in = new HashMap<>();
							// 设备名称+型号+计量单位
							String str = (Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName())
									+ (Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel())
									+ (Common.isEmpty(st.getStock().getUnit()) ? ""
											: st.getStock().getUnit().getName());

							in.put("itemNo", Common.isEmpty(stock.getItemNo()) ? "" : stock.getItemNo());
//							in.put("id", Common.isEmpty(st.getId()) ? "" : st.getId());
							in.put("id", createIdByStr(str));
							inN++;
							in.put("dj", createDJ(end, inN, ""));
							in.put("cg", "赊购");
							in.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
							in.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
							in.put("stockName", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
							in.put("modelName",
									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
//							in.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
							// 单价=入库总金额/入库数量
							in.put("price", devide(st.getInprice(), st.getNum()));
							in.put("unit",
									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
							in.put("depotTime", st.getStorageTime().substring(0, 10));
							in.put("num", st.getNum());
							in.put("purchaseInvoiceNo",
									Common.isEmpty(st.getPurchaseInvoiceNo()) ? "" : st.getPurchaseInvoiceNo());
							in.put("supplier", Common.isEmpty(st.getStock().getSupplier()) ? ""
									: st.getStock().getSupplier().getName());

							in.put("purchaseInvoiceDate",
									Common.isEmpty(st.getPurchaseInvoiceDate()) ? "" : st.getPurchaseInvoiceDate());

							inlist.add(in);
						} else {
							Map<String, Object> out = new HashMap<>();
							// 设备名称+型号+计量单位
							String str = (Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName())
									+ (Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel())
									+ (Common.isEmpty(st.getStock().getUnit()) ? ""
											: st.getStock().getUnit().getName());

							out.put("itemNo", Common.isEmpty(stock.getItemNo()) ? "" : stock.getItemNo());
//							out.put("id", Common.isEmpty(st.getId()) ? "" : st.getId());
							out.put("id", createIdByStr(str));
							OutN++;
							out.put("dj", createDJ(end, OutN, ""));
							out.put("fs", "赊销");
							out.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
							out.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
							out.put("stockName",
									Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
							out.put("modelName",
									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
							out.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
							out.put("unit",
									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
							out.put("depotTime", st.getDepotTime().substring(0, 10));
							out.put("num", st.getNum());
							out.put("sailesInvoiceNo",
									Common.isEmpty(st.getSailesInvoiceNo()) ? "" : st.getSailesInvoiceNo());
							out.put("sailesInvoiceDate",
									Common.isEmpty(st.getSailesInvoiceDate()) ? "" : st.getSailesInvoiceDate());
							out.put("receiptNo", Common.isEmpty(st.getReceiptNo()) ? "" : st.getReceiptNo());
							out.put("customer", Common.isEmpty(st.getCustomer()) ? "" : st.getCustomer());
							out.put("purchaseInvoiceDate",
									Common.isEmpty(st.getPurchaseInvoiceDate()) ? "" : st.getPurchaseInvoiceDate());
							outlist.add(out);

						}

					}
				}
			}
		}

		Map<String, Object> dataMap = new HashMap<>();

		dataMap.put("inlist", inlist);
		dataMap.put("outlist", outlist);

		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
		String fileName = "金蝶库存统计导出模板.xlsx";
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

	// 生成固定的单据编号
	public String createDJ(String end, Integer i, String tou) {
		String endnew = end.replace("-", "").substring(0, 8);
		return tou + endnew + StringUtils.leftPad(String.valueOf(i), 8, '0');
	}

	public String createIdByStr(String str) {
		String newStr = str.replace(".", "");
		return PinyinTool.getPinYinHeadChar(newStr);
	}

	public BigDecimal devide(Double price, Double num1) {
		if (Common.isEmpty(price)) {
			return new BigDecimal(0);
		}
		BigDecimal inprice = new BigDecimal(price);
		BigDecimal num = new BigDecimal(num1);
		return inprice.divide(num, 2, BigDecimal.ROUND_HALF_UP);
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
			String end, String type, String areaId, String searchAgent) {

		int j = 1;

		List<Stock> listStock = this.findStocksBySearch(search, areaId, searchAgent);
		// 获取所有的库存
		List<StockStatistics> list = this.findStockStatistics(search, start, end, type, areaId, searchAgent);
		String msg = "";
		if (type.equals("in")) {
			// 入库统计
			msg = "入库:";
		} else if (type.equals("out")) {
			// 出库统计
			msg = "出库:";
		}
		for (Stock stock : listStock) {
			// 获取所有的设备
			HSSFRow row = sheet.createRow(j + 1);

			HSSFCell cell = row.createCell(0);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getArea() != null ? stock.getArea().getName() : "");

			cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getName());

			cell = row.createCell(2);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getModel());

			int l = 2;

			for (StockStatistics st : list) {

				if (st.getStock() != null) {
					if (stock.getId().equals(st.getStock().getId())) {
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
		list.add("设备名称");
		list.add("品名型号");
		list.add("日期");
		list.add("数量");
		return list;
	}

	@SystemServiceLog(description = "根据条件查询库存统计-findStockStatistics")
	public List<StockStatistics> findStockStatistics(String search, String start, String end, String type,
			String areaId, String searchAgent) {
		Query query = new Query();

		if (Common.isNotEmpty(searchAgent)) {
			query = query.addCriteria(Criteria.where("agent").is(Boolean.valueOf(searchAgent)));
		}
		query = this.findbySearch(search, start, end, type, query, areaId, "");
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		query.addCriteria(Criteria.where("revoke").is(false));
		List<StockStatistics> list = this.find(query, StockStatistics.class);
		return list;
	}

	@Override
	@SystemServiceLog(description = "根据条件查询库存统计-findAllByDate")
	public List<StockStatistics> findAllByDate(String date, boolean inOrOut) {
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
		List<StockStatistics> list = this.find(query, StockStatistics.class);
		return list;
	}

	@Override
	public List<StockStatistics> findAllStockStatics() {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		return this.find(query, StockStatistics.class);
	}

	@Override
	public byte[] exportWord(String id, HttpServletRequest request, HttpSession session) {
		// 根据id获取出库商品
		StockStatistics stockStatistics = this.findOneById(id, StockStatistics.class);
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		Map<String, Object> dataMap = wordGeneralMessage(stockStatistics, user);
		long allnum = 0;
		List<Map<String, Object>> stocks = new ArrayList<>();
		Map<String, Object> stock;
		// 根据stockStatistics获取订单号
//		if (Common.isEmpty(stockStatistics.getOutboundOrder())) {
//			System.out.println(stockStatistics.getStock() == null);
//			// 如果订单号为空说明是1个设备（历史数据处理）
//			if (stockStatistics.getStock() != null) {
//				stock = new HashMap<>();
//				stock.put("id", 1);
//				stock.put("name", Common.isEmpty(stockStatistics.getStock().getName()) ? ""
//						: stockStatistics.getStock().getName());
//				stock.put("model", Common.isEmpty(stockStatistics.getStock().getModel()) ? ""
//						: stockStatistics.getStock().getModel());
//				stock.put("unitName",
//						Common.isNotEmpty(stockStatistics.getStock().getUnit())
//								? stockStatistics.getStock().getUnit().getName()
//								: "");
//				stock.put("num", stockStatistics.getNum());
//				allnum = stockStatistics.getNum();
//				stocks.add(stock);
//			}
//		} else {
//			// 根据单号获取所有出库数据
//			List<StockStatistics> stockStatisticsList = this.findByoutboundOrder(stockStatistics.getOutboundOrder());
//
//			for (int i = 0; i < stockStatisticsList.size(); i++) {
//				stock = new HashMap<>();
//				stock.put("id", i + 1);
//				stock.put("name", stockStatisticsList.get(i).getStock().getName());
//				stock.put("model", stockStatisticsList.get(i).getStock().getModel());
//				stock.put("unitName",
//						Common.isNotEmpty(stockStatisticsList.get(i).getStock().getUnit())
//								? stockStatisticsList.get(i).getStock().getUnit().getName()
//								: "");
//				stock.put("num", stockStatisticsList.get(i).getNum());
//				allnum += stockStatisticsList.get(i).getNum();
//				stocks.add(stock);
//			}
//
//		}

		// 通过项目id 客户id 领料人来获取数据 new
		// 根据单号获取所有出库数据
		List<StockStatistics> stockStatisticsList = this.findStockStatisticsToCreateQrcode(stockStatistics.getPname(),
				stockStatistics.getNewCustomer(), stockStatistics.getAccepter(), stockStatistics.getDepotTime());

		for (int i = 0; i < stockStatisticsList.size(); i++) {
			stock = new HashMap<>();
			stock.put("id", i + 1);
			stock.put("name", stockStatisticsList.get(i).getStock().getName());
			stock.put("model", stockStatisticsList.get(i).getStock().getModel());
			stock.put("unitName",
					Common.isNotEmpty(stockStatisticsList.get(i).getStock().getUnit())
							? stockStatisticsList.get(i).getStock().getUnit().getName()
							: "");
			stock.put("num", stockStatisticsList.get(i).getNum());
			allnum += stockStatisticsList.get(i).getNum();
			stocks.add(stock);
		}

		dataMap.put("allnum", allnum);
		dataMap.put("stocks", stocks);

		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
		String fileName = "销货单.docx";

		byte[] doc = null;

		try {
			doc = WordUtil.exportWord(ctxPath + fileName, dataMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return doc;

	}

	/**
	 * 
	 * @param stockStatistics
	 * @param user
	 * @return 生成word通用消息内容
	 */
	public Map<String, Object> wordGeneralMessage(StockStatistics stockStatistics, User user) {
		Map<String, Object> dataMap = new HashMap<>();
		ImageEntity image = new ImageEntity();
		image.setHeight(40);
		image.setWidth(80);
		if (Common.isNotEmpty(stockStatistics.getMysign())) {
			byte[] b = Base64.getDecoder()
					.decode(stockStatistics.getMysign().getSign().replace("data:image/png;base64,", ""));
			image.setData(b);
			image.setType(ImageEntity.Data);
			dataMap.put("image", image);
		} else {
			dataMap.put("image", " ");

		}
		ImageEntity oimage = new ImageEntity();
		oimage.setHeight(40);
		oimage.setWidth(80);
		if (Common.isNotEmpty(stockStatistics.getOthersign())) {
			byte[] b = Base64.getDecoder()
					.decode(stockStatistics.getOthersign().getSign().replace("data:image/png;base64,", ""));
			oimage.setData(b);
			oimage.setType(ImageEntity.Data);
			dataMap.put("otherimage", oimage);
		} else {
			dataMap.put("otherimage", " ");

		}

//		String outboundOrder = Common.isEmpty(stockStatistics.getOutboundOrder()) ? ""
//				: stockStatistics.getOutboundOrder();
		dataMap.put("customer",
				Common.isEmpty(stockStatistics.getNewCustomer()) ? "" : stockStatistics.getNewCustomer().getName());
		dataMap.put("personInCharge",
				Common.isEmpty(stockStatistics.getPname()) ? "" : stockStatistics.getPname().getName());
		try {
			dataMap.put("createDate", Common.getDateYMDHM(stockStatistics.getDepotTime()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		dataMap.put("outboundOrder", outboundOrder);
		// 领货人

		dataMap.put("accepter", Common.isEmpty(stockStatistics.getAccepter()) ? "" : stockStatistics.getAccepter());
//		dataMap.put("description",
//				Common.isEmpty(stockStatistics.getDescription()) ? "" : stockStatistics.getDescription());
		dataMap.put("username", Common.isEmpty(user.getUserName()) ? "" : user.getUserName());
		dataMap.put("projectName",
				Common.isEmpty(stockStatistics.getPname()) ? "" : stockStatistics.getPname().getName());

//		  dataMap.put("customer",stockStatistics.getCustomer());
//		  dataMap.put("personInCharge", stockStatistics.getPersonInCharge());
//		  try {
//			  dataMap.put("createDate", Common.getDateYMDH(stockStatistics.getDepotTime()));
//		  } catch (ParseException e) {
//			  // TODO Auto-generated catch block
//			  e.printStackTrace();
//		  }
//		  dataMap.put("outboundOrder",outboundOrder);
//		  dataMap.put("description", stockStatistics.getDescription());
//		  dataMap.put("username", user.getUserName());
//		  dataMap.put("projectName", stockStatistics.getProjectName());
		return dataMap;
	}

	@Override
	public List<StockStatistics> findByoutboundOrder(String outboundOrder) {
		Query query = new Query();
		query.addCriteria(Criteria.where("outboundOrder").is(outboundOrder));
		query.addCriteria(Criteria.where("revoke").is(false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		return this.find(query, StockStatistics.class);

	}

	public QrCode createStockStatisticsQrCode(String stockStatisticsId) {
		StockStatistics stock = null;
		String time = "";
		if (Common.isNotEmpty(stockStatisticsId)) {
			stock = this.findOneById(stockStatisticsId, StockStatistics.class);
			time = stock.getDepotTime().replaceAll(":", "");
			if (stock.getQrCode() != null) {
				// 判断二维码是否存在，不存在则重新创建
				String downLoadPath = stock.getQrCode().getQrcode().getDir()
						+ stock.getQrCode().getQrcode().getSavePath() + stock.getQrCode().getQrcode().getOriginalName();
				File f = new File(downLoadPath);
				if (!f.exists()) {
					stock.setQrCode(null);
				}
			}

		}
		if (stock == null) {
			return null;
		}

		QrCode qrcode = null;
		if (stock.getQrCode() == null) {
			qrcode = new QrCode();
			try {
				Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
				hints.put(EncodeHintType.CHARACTER_SET, "utf-8"); // 内容所使用字符集编码
				String urlpath = "wechat/batchOut/" + stockStatisticsId;
				String url = weburl + urlpath;
				BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height, hints);
				// 生成二维码
				String path = dir + qrcodepath + "/";
				Common.checkPathAndMkdirs(path);
				String projectname = Common.isNotEmpty(stock.getPname()) ? stock.getPname().getName() : "";
				String customer = Common.isNotEmpty(stock.getNewCustomer()) ? stock.getNewCustomer().getName() : "";
				String accepter = Common.isNotEmpty(stock.getAccepter()) ? stock.getAccepter() : "";
				projectname = projectname.replaceAll("[\\/:*?\"<>|]", "@");
				customer = customer.replaceAll("[\\/:*?\"<>|]", "@");
				accepter = accepter.replaceAll("[\\/:*?\"<>|]", "@");
//				if (stock.getOutboundOrder() != null) {
				File outputFile = new File(path + "-" + projectname + "-" + customer + "-" + accepter + time + ".png");
				MatrixToImageWriter.writeToFile(bitMatrix, format, outputFile);
				// 保存图片信息
				MultiMedia saveQrCode = this.multiMediaService.saveQrCode(outputFile, dir, qrcodepath, "PHOTO");
				qrcode.setQrcode(saveQrCode);
				qrcode.setPath(urlpath);
				qrcode.setName(projectname + customer + time);
				qrcode.setType("STOCKSTATISTICS");
				this.qrCodeServce.insert(qrcode);
				stock.setQrCode(qrcode);
				this.save(stock);
//				}

			} catch (WriterException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return stock.getQrCode();
	}

	@Override
	public StockStatistics createStockStatisticsQrCodeAndDownload(String id) {

		StockStatistics stockStatistics = this.findOneById(id, StockStatistics.class);

		// 老版本 通过订单号outboundorder
		// List<StockStatistics> findByoutboundOrder =
		// this.findByoutboundOrder(stockStatistics.getOutboundOrder());
		// 通过出库统计中 项目id 客户id 领料人 来获取所有出库统计作为一个出库二维码

		List<StockStatistics> sts = this.findStockStatisticsToCreateQrcode(stockStatistics.getPname(),
				stockStatistics.getNewCustomer(), stockStatistics.getAccepter(), stockStatistics.getDepotTime());

		sts.forEach(o -> {
			this.createStockStatisticsQrCode(o.getId());
		});

		return this.findOneById(id, StockStatistics.class);

	}

	@Override
	public Map<Object, Object> stockStatisticsPickup(StockStatistics stockStatistics, String i) {
		// 根据stockStatisticsid查询
		StockStatistics st = this.findOneById(stockStatistics.getId(), StockStatistics.class);

//		String outboundOrder = stockStatistics.getOutboundOrder();
//		List<StockStatistics> st = this.findByoutboundOrder(outboundOrder);
		List<StockStatistics> findStockStatisticsToCreateQrcode = this.findStockStatisticsToCreateQrcode(st.getPname(),
				st.getNewCustomer(), st.getAccepter(), st.getDepotTime());

		Map<Object, Object> map = new HashMap<>();
		map.put("personInCharge", Common.isNotEmpty(st.getPname()) ? st.getPname().getPm() : "");
		map.put("projectName", Common.isNotEmpty(st.getPname()) ? st.getPname().getName() : "");
		map.put("customer", Common.isNotEmpty(st.getNewCustomer()) ? st.getNewCustomer().getName() : "");
		map.put("accepter", st.getAccepter());
		map.put("description", st.getDescription());
		map.put("pName", st.getPname());
		map.put("newCustomer", st.getNewCustomer());
//		map.put("outboundOrder", st.getOutboundOrder());

		if (i.equals("sign")) {
			// 已经签名2个不用判断
			map.put("sign", st.getMysign().getSign());
			map.put("othersign", st.getOthersign().getSign());
			map.put("time", st.getPickupTime());
		} else {
			try {
				String dateYMDHM = Common.getDateYMDHM(new Date());
				stockStatistics.setPickupTime(dateYMDHM);

				map.put("time", dateYMDHM);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (Common.isEmpty(st.getMysign()) && Common.isEmpty(st.getOthersign())) {
				map.put("sign", stockStatistics.getSign());
				// 保存签名
				Sign sign = new Sign();
				sign.setSign(stockStatistics.getSign());
				this.signService.save(sign);

				findStockStatisticsToCreateQrcode.forEach(s -> {
					s.setMysign(sign);
					s.setOpenId(stockStatistics.getOpenId());
					s.setPickupTime(stockStatistics.getPickupTime());
					this.save(s);
				});
			} else {
				// sign不为空，othersign为空的情况下，将form表单中的sign传入othersign，且保存pthersign
				map.put("sign", st.getMysign().getSign());
				map.put("othersign", stockStatistics.getSign());
				// 保存签名
				Sign sign = new Sign();
				sign.setSign(stockStatistics.getSign());
				this.signService.save(sign);
				findStockStatisticsToCreateQrcode.forEach(s -> {
					s.setOthersign(sign);
					s.setOpenId(stockStatistics.getOpenId());
					s.setPickupTime(stockStatistics.getPickupTime());
					this.save(s);
				});
			}
		}

		return map;
	}

	@Override
	public HSSFWorkbook export(String search, String start, String end, String type, String name, String areaId,
			String searchAgent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateStockStatistics(String ids, String price, Double inprice, String purchaseInvoiceNo,
			String receiptNo, String paymentOrderNo, String sailesInvoiceNo, String sailesInvoiceDate, User user,
			String purchaseInvoiceDate, Double sailPrice, String newItemNo, String description) {

		List<String> array = Arrays.asList(ids.split(","));

		for (String id : array) {
			StockStatistics stockStatistics = this.findOneById(id, StockStatistics.class);

			if (inprice != null) {
				stockStatistics.setInprice(inprice);
			}
			if (price != null) {
				stockStatistics.setPrice(price);
			}
			if (!purchaseInvoiceNo.equals("null")) {
				stockStatistics.setPurchaseInvoiceNo(purchaseInvoiceNo);
			}
			if (!receiptNo.equals("null")) {
				stockStatistics.setReceiptNo(receiptNo);
			}
			if (!paymentOrderNo.equals("null")) {
				stockStatistics.setPaymentOrderNo(paymentOrderNo);
			}
			if (!sailesInvoiceNo.equals("null")) {
				stockStatistics.setSailesInvoiceNo(sailesInvoiceNo);
			}
			if (!sailesInvoiceDate.equals("null")) {
				stockStatistics.setSailesInvoiceDate(sailesInvoiceDate);
			}
			if (!purchaseInvoiceDate.equals("null")) {
				stockStatistics.setPurchaseInvoiceDate(purchaseInvoiceDate);
			}
			if (sailPrice != null) {
				stockStatistics.setSailPrice(sailPrice);
			}
			if (!newItemNo.equals("null")) {
				stockStatistics.setNewItemNo(newItemNo);
			}
			if (!description.equals("null")) {
				stockStatistics.setDescription(description);
			}
			stockStatistics.setEditFinanceTime(Common.fromDateH());
			stockStatistics.setFinanceUser(user);
			this.save(stockStatistics);
		}

	}

	// TODO //AI修改
	@Override
	public Workbook newExport2(HttpServletRequest request, RequestBo requestBo) {
	    Query query = new Query();
	    String end = "";
	    boolean isCurrentMonth = false; // 【新增】标记是否为当月查询
	    if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
	        end = requestBo.getEnd() + " 23:59:59";
	        isCurrentMonth = Common.isDateTimeInCurrentMonth(end); // 【新增】判断是否为当月

	        Criteria ca = new Criteria();
	        ca.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
	                Criteria.where("depotTime").gte(requestBo.getStart()).lte(end));
	        query.addCriteria(ca);
	    }

	    query.addCriteria(Criteria.where("revoke").is(false));
	    query.addCriteria(Criteria.where("byRevoke").is(false));
	    List<StockStatistics> list = this.find(query, StockStatistics.class);
	    List<Map<String, Object>> outlist = new ArrayList<>();

	    // ========== 1. 查询历史月度统计数据（原有逻辑保留） ==========
	    String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(requestBo.getStart());
	    List<MonthEndStatistics> findMonthEndStatisticsByDate = this.monthEndStatisticsService
	            .findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
	    Map<String, MonthEndStatistics> listMonthEndStatisticsToMap = listMonthEndStatisticsToMap(
	            findMonthEndStatisticsByDate);

	    // ========== 2. 新增：查询当前查询月份的月度统计数据 ==========
	    Map<String, MonthEndStatistics> currentQueryMonthStatsMap = new HashMap<>();
	    if (!isCurrentMonth && Common.isNotEmpty(requestBo.getEnd())) {
	        // 获取查询月份最后一天（复用Common工具类方法）
	        String queryMonthLastDay = Common.getLastDayOfCurrentMonthAsString(requestBo.getEnd());
	        // 查询该日期的月度统计数据
	        List<MonthEndStatistics> currentMonthStatsList = this.monthEndStatisticsService
	                .findMonthEndStatisticsByDate(queryMonthLastDay);
	        // 转换为Map（设备ID为key）
	        currentQueryMonthStatsMap = listMonthEndStatisticsToMap(currentMonthStatsList);
	    }

	    // 获取所有库存设备
	    Map<String, List<StockStatistics>> map = new HashMap<String, List<StockStatistics>>();
	    List<Stock> findAllStock = new ArrayList<Stock>();
	    if (Common.isDateTimeInCurrentMonth(end)) {
	        findAllStock = this.stockService.findAllStock();
	        List<StockStatistics> list2 = findAllStock.stream().map(stock -> {
	            StockStatistics st = new StockStatistics();
	            st.setNewNum(0.0);
	            st.setNum(0.0);
	            st.setStock(stock);
	            return st;
	        }).collect(Collectors.toList());
	        list.addAll(list2);
	    } else {
	        findAllStock = this.stockService.findAllStock();
	        List<StockStatistics> list2 = findMonthEndStatisticsByDate.stream().map(stock -> {
	            StockStatistics st = new StockStatistics();
	            st.setNewNum(stock.getMonthEndStockNum());
	            st.setNum(0.0);
	            st.setStock(stock.getStock());
	            return st;
	        }).collect(Collectors.toList());
	        list.addAll(list2);
	    }

	    // 构建设备库存统计Map
	    for (StockStatistics st : list) {
	        String stockId = st.getStock().getId();
	        boolean containsKey = map.containsKey(stockId);
	        if (containsKey) {
	            List<StockStatistics> mlist = map.get(stockId);
	            mlist.add(st);
	        } else {
	            List<StockStatistics> ls = new ArrayList<StockStatistics>();
	            ls.add(st);
	            map.put(stockId, ls);
	        }
	    }

	    // 遍历处理每个设备的统计数据
	    for (Map.Entry<String, List<StockStatistics>> entry : map.entrySet()) {
	        Map<String, Object> outmap = new HashMap<>();
	        StockStatistics gs = entry.getValue().get(entry.getValue().size() - 1);

	        // 设备基础信息赋值
	        outmap.put("area", Common.isEmpty(gs.getStock().getArea()) ? "" : gs.getStock().getArea().getName());
	        outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
	        outmap.put("modelName", Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
	        outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
	        if (Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
	            outmap.put("goodsStorage",
	                    Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())
	                            ? "/" + gs.getStock().getGoodsStorage().getShelflevel()
	                            : "");
	        }
	        outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent() ? "是" : "否");
	        outmap.put("unit", Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());

	        // 初始化变量
	        BigDecimal oldDj = new BigDecimal(0);// 期初单价
	        BigDecimal zj = new BigDecimal(0);// 期初总金额
	        BigDecimal newNum = new BigDecimal(gs.getNewNum());
	        BigDecimal num = new BigDecimal(gs.getNum());
	        BigDecimal qcnum = new BigDecimal(0);
	        MonthEndStatistics monthEndStatistics = listMonthEndStatisticsToMap.get(gs.getStock().getId());
	        if (Common.isNotEmpty(monthEndStatistics)) {
	            qcnum = new BigDecimal(monthEndStatistics.getMonthEndStockNum());
	        }

	        // ====================== 期初单价逻辑（原有逻辑保留） ======================
	        if (isCurrentMonth) {
	            // 当月：维持原有逻辑
	            if (Common.isNotEmpty(gs.getStock().getPrice())) {
	                String price = gs.getStock().getPrice();
	                boolean numeric = Common.isNumeric(price);
	                if (!numeric) {
	                    price = "0.00";
	                }
	                oldDj = new BigDecimal(price);
	                zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
	            } else {
	                oldDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
	                zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
	            }
	        } else {
	            // 非当月：优先历史价格，零值/空值则降级到Stock
	            boolean useHistoryPrice = false;
	            if (Common.isNotEmpty(monthEndStatistics) && Common.isNotEmpty(monthEndStatistics.getPrice())) {
	                String historyPrice = monthEndStatistics.getPrice();
	                // 校验历史价格有效性 + 判断是否为0/0.00
	                boolean isValidPrice = Common.isNumeric(historyPrice);
	                BigDecimal historyPriceBig = isValidPrice ? new BigDecimal(historyPrice) : new BigDecimal("0.00");
	                // 仅当历史价格有效且≠0时使用
	                if (isValidPrice && historyPriceBig.compareTo(BigDecimal.ZERO) != 0) {
	                    oldDj = historyPriceBig;
	                    zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
	                    useHistoryPrice = true;
	                }
	            }
	            // 降级逻辑：历史价格为0/空/无效时，从Stock获取
	            if (!useHistoryPrice) {
	                if (Common.isNotEmpty(gs.getStock().getPrice())) {
	                    String stockPrice = gs.getStock().getPrice();
	                    boolean numeric = Common.isNumeric(stockPrice);
	                    if (!numeric) {
	                        stockPrice = "0.00";
	                    }
	                    oldDj = new BigDecimal(stockPrice);
	                    zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
	                } else {
	                    oldDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
	                    zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
	                }
	            }
	        }

	        // 期初数据存入map（保持原有正确数据）
	        outmap.put("oldprice", oldDj);
	        outmap.put("oldinventory", qcnum);
	        outmap.put("oldpriceall", zj);

	        // 计算入库/出库数量
	        long in = 0;
	        long out = 0;
	        for (StockStatistics st : entry.getValue()) {
	            if (st.isInOrOut()) {
	                in += st.getNum();
	            } else {
	                out += st.getNum();
	            }
	        }

	        // ====================== 重构入库单价（inprice）逻辑（核心新增） ======================
	        BigDecimal inPrice = BigDecimal.ZERO; // 最终入库单价（取自MonthEndStatistics的inprice）
	        if (isCurrentMonth) {
	            // 当月：优先计算当月平均单价，兼容原有逻辑
	            inPrice = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
	        } else {
	            // 历史月份：优先从查询月份的月度统计中取inprice
	            MonthEndStatistics historyMonthStats = currentQueryMonthStatsMap.get(gs.getStock().getId());
	            if (historyMonthStats != null && Common.isNotEmpty(historyMonthStats.getInprice()) 
	                    && Common.isNumeric(historyMonthStats.getInprice())) {
	                // 核心：使用MonthEndStatistics中的inprice字段
	                inPrice = new BigDecimal(historyMonthStats.getInprice());
	            } else if (historyMonthStats != null && Common.isNotEmpty(historyMonthStats.getPrice()) 
	                    && Common.isNumeric(historyMonthStats.getPrice())) {
	                // 降级：使用历史月份的price字段
	                inPrice = new BigDecimal(historyMonthStats.getPrice());
	            } else if (oldDj.compareTo(BigDecimal.ZERO) != 0) {
	                // 再降级：使用期初单价
	                inPrice = oldDj;
	            } else if (Common.isNotEmpty(gs.getStock().getPrice()) && Common.isNumeric(gs.getStock().getPrice())) {
	                // 最终降级：使用库存基础价格
	                inPrice = new BigDecimal(gs.getStock().getPrice());
	            } else {
	                // 兜底：计算平均单价
	                inPrice = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
	            }
	        }

	        // ====================== 变动单价（changeDj）逻辑（原有逻辑保留，仅调整变量名） ======================
	        BigDecimal changeDj = BigDecimal.ZERO; // 出库使用的变动单价（原有逻辑）
	        if (isCurrentMonth) {
	            changeDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
	        } else {
	            MonthEndStatistics historyMonthStats = currentQueryMonthStatsMap.get(gs.getStock().getId());
	            
	            if (historyMonthStats != null && Common.isNotEmpty(historyMonthStats.getPrice()) 
	                    && Common.isNumeric(historyMonthStats.getPrice())) {
	                changeDj = new BigDecimal(historyMonthStats.getPrice());
	            } else if (oldDj.compareTo(BigDecimal.ZERO) != 0) {
	                changeDj = oldDj;
	            } else if (Common.isNotEmpty(gs.getStock().getPrice()) && Common.isNumeric(gs.getStock().getPrice())) {
	                changeDj = new BigDecimal(gs.getStock().getPrice());
	            } else {
	                changeDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
	            }
	        }

	        // ====================== 入库金额计算（改用新的inPrice） ======================
	        BigDecimal inBig = new BigDecimal(in);
	        BigDecimal inpriceall = BigDecimal.ZERO;
	        if (in > 0) {
	            inpriceall = inBig.multiply(inPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
	        }
	        outmap.put("in", in);
	        outmap.put("inprice", inPrice); // 存入MonthEndStatistics的inprice
	        outmap.put("inpriceall", inpriceall);

	        // ====================== 出库单价逻辑（完全保留原有逻辑） ======================
	        BigDecimal outprice = BigDecimal.ZERO;
	        BigDecimal denominator = qcnum.add(inBig); // 分母：期初数量 + 入库数量
	        
	        if(isCurrentMonth) {
	        	  if (denominator.compareTo(BigDecimal.ZERO) > 0) { // 防止除数为0
		            outprice = zj.add(inpriceall).divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
		        } else if (changeDj.compareTo(BigDecimal.ZERO) > 0) { // 降级使用变动单价
		            outprice = changeDj;
		        } else {
		            outprice = oldDj; // 最终降级使用期初单价
		        }
	        }else {
	        	outprice = changeDj;
	        }

	        // 计算出库金额
	        BigDecimal outBig = new BigDecimal(out);
	        BigDecimal outpriceall = outBig.multiply(outprice).setScale(2, BigDecimal.ROUND_HALF_UP);
	        outmap.put("out", out);
	        outmap.put("outprice", outprice); // outprice保持原有逻辑不变
	        outmap.put("outpriceall", outpriceall);

	        // 计算期末库存数量（原有逻辑保留）
	        BigDecimal qmnum = qcnum.add(inBig).subtract(outBig);
	        
	        // 期末总价计算逻辑（原有逻辑保留）
	        BigDecimal qmzj = qmnum.multiply(outprice).setScale(2, BigDecimal.ROUND_DOWN);
	        outmap.put("newprice", outprice);
	        outmap.put("newinventory", qmnum);
	        outmap.put("newpriceall", qmzj); 
	        outmap.put("purchaseInvoiceDate",
	                Common.isEmpty(gs.getPurchaseInvoiceDate()) ? "" : gs.getPurchaseInvoiceDate());
	        outlist.add(outmap);
	    }

	    // 导出Excel
	    Map<String, Object> dataMap = new HashMap<>();
	    dataMap.put("inlist", outlist);
	    dataMap.put("title", requestBo.getStart() + "~" + requestBo.getEnd() + "库存统计");

	    String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
	    String fileName = "新库存统计导出模板.xlsx";
	    TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
	    Workbook doc = null;

	    try {
	        doc = ExcelExportUtil.exportExcel(params, dataMap);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return doc;
	}
//	public Workbook newExport2(HttpServletRequest request, RequestBo requestBo) {
//	    Query query = new Query();
//	    String end = "";
//	    boolean isCurrentMonth = false; // 【新增】标记是否为当月查询
//	    if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
//	        end = requestBo.getEnd() + " 23:59:59";
//	        isCurrentMonth = Common.isDateTimeInCurrentMonth(end); // 【新增】判断是否为当月
//
//	        Criteria ca = new Criteria();
//	        ca.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
//	                Criteria.where("depotTime").gte(requestBo.getStart()).lte(end));
//	        query.addCriteria(ca);
//	    }
//
//	    query.addCriteria(Criteria.where("revoke").is(false));
//	    query.addCriteria(Criteria.where("byRevoke").is(false));
//	    List<StockStatistics> list = this.find(query, StockStatistics.class);
//	    List<Map<String, Object>> outlist = new ArrayList<>();
//
//	    // ========== 1. 查询历史月度统计数据（原有逻辑保留） ==========
//	    String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(requestBo.getStart());
//	    List<MonthEndStatistics> findMonthEndStatisticsByDate = this.monthEndStatisticsService
//	            .findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
//	    Map<String, MonthEndStatistics> listMonthEndStatisticsToMap = listMonthEndStatisticsToMap(
//	            findMonthEndStatisticsByDate);
//
//	    // ========== 2. 新增：查询当前查询月份的月度统计数据 ==========
//	    Map<String, MonthEndStatistics> currentQueryMonthStatsMap = new HashMap<>();
//	    if (!isCurrentMonth && Common.isNotEmpty(requestBo.getEnd())) {
//	        // 获取查询月份最后一天（复用Common工具类方法）
//	        String queryMonthLastDay = Common.getLastDayOfCurrentMonthAsString(requestBo.getEnd());
//	        // 查询该日期的月度统计数据
//	        List<MonthEndStatistics> currentMonthStatsList = this.monthEndStatisticsService
//	                .findMonthEndStatisticsByDate(queryMonthLastDay);
//	        // 转换为Map（设备ID为key）
//	        currentQueryMonthStatsMap = listMonthEndStatisticsToMap(currentMonthStatsList);
//	    }
//
//	    // 获取所有库存设备
//	    Map<String, List<StockStatistics>> map = new HashMap<String, List<StockStatistics>>();
//	    List<Stock> findAllStock = new ArrayList<Stock>();
//	    if (Common.isDateTimeInCurrentMonth(end)) {
//	        findAllStock = this.stockService.findAllStock();
//	        List<StockStatistics> list2 = findAllStock.stream().map(stock -> {
//	            StockStatistics st = new StockStatistics();
//	            st.setNewNum(0.0);
//	            st.setNum(0.0);
//	            st.setStock(stock);
//	            return st;
//	        }).collect(Collectors.toList());
//	        list.addAll(list2);
//	    } else {
//	        findAllStock = this.stockService.findAllStock();
//	        List<StockStatistics> list2 = findMonthEndStatisticsByDate.stream().map(stock -> {
//	            StockStatistics st = new StockStatistics();
//	            st.setNewNum(stock.getMonthEndStockNum());
//	            st.setNum(0.0);
//	            st.setStock(stock.getStock());
//	            return st;
//	        }).collect(Collectors.toList());
//	        list.addAll(list2);
//	    }
//
//	    // 构建设备库存统计Map
//	    for (StockStatistics st : list) {
//	        String stockId = st.getStock().getId();
//	        boolean containsKey = map.containsKey(stockId);
//	        if (containsKey) {
//	            List<StockStatistics> mlist = map.get(stockId);
//	            mlist.add(st);
//	        } else {
//	            List<StockStatistics> ls = new ArrayList<StockStatistics>();
//	            ls.add(st);
//	            map.put(stockId, ls);
//	        }
//	    }
//
//	    // 遍历处理每个设备的统计数据
//	    for (Map.Entry<String, List<StockStatistics>> entry : map.entrySet()) {
//	        Map<String, Object> outmap = new HashMap<>();
//	        StockStatistics gs = entry.getValue().get(entry.getValue().size() - 1);
//
//	        // 设备基础信息赋值
//	        outmap.put("area", Common.isEmpty(gs.getStock().getArea()) ? "" : gs.getStock().getArea().getName());
//	        outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
//	        outmap.put("modelName", Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
//	        outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
//	        if (Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
//	            outmap.put("goodsStorage",
//	                    Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())
//	                            ? "/" + gs.getStock().getGoodsStorage().getShelflevel()
//	                            : "");
//	        }
//	        outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent() ? "是" : "否");
//	        outmap.put("unit", Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());
//
//	        // 初始化变量
//	        BigDecimal oldDj = new BigDecimal(0);// 期初单价
//	        BigDecimal zj = new BigDecimal(0);// 期初总金额
//	        BigDecimal newNum = new BigDecimal(gs.getNewNum());
//	        BigDecimal num = new BigDecimal(gs.getNum());
//	        BigDecimal qcnum = new BigDecimal(0);
//	        MonthEndStatistics monthEndStatistics = listMonthEndStatisticsToMap.get(gs.getStock().getId());
//	        if (Common.isNotEmpty(monthEndStatistics)) {
//	            qcnum = new BigDecimal(monthEndStatistics.getMonthEndStockNum());
//	        }
//
//	        // ====================== 期初单价逻辑（原有逻辑保留） ======================
//	        if (isCurrentMonth) {
//	            // 当月：维持原有逻辑
//	            if (Common.isNotEmpty(gs.getStock().getPrice())) {
//	                String price = gs.getStock().getPrice();
//	                boolean numeric = Common.isNumeric(price);
//	                if (!numeric) {
//	                    price = "0.00";
//	                }
//	                oldDj = new BigDecimal(price);
//	                zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
//	            } else {
//	                oldDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
//	                zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
//	            }
//	        } else {
//	            // 非当月：优先历史价格，零值/空值则降级到Stock
//	            boolean useHistoryPrice = false;
//	            if (Common.isNotEmpty(monthEndStatistics) && Common.isNotEmpty(monthEndStatistics.getPrice())) {
//	                String historyPrice = monthEndStatistics.getPrice();
//	                // 校验历史价格有效性 + 判断是否为0/0.00
//	                boolean isValidPrice = Common.isNumeric(historyPrice);
//	                BigDecimal historyPriceBig = isValidPrice ? new BigDecimal(historyPrice) : new BigDecimal("0.00");
//	                // 仅当历史价格有效且≠0时使用
//	                if (isValidPrice && historyPriceBig.compareTo(BigDecimal.ZERO) != 0) {
//	                    oldDj = historyPriceBig;
//	                    zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
//	                    useHistoryPrice = true;
//	                }
//	            }
//	            // 降级逻辑：历史价格为0/空/无效时，从Stock获取
//	            if (!useHistoryPrice) {
//	                if (Common.isNotEmpty(gs.getStock().getPrice())) {
//	                    String stockPrice = gs.getStock().getPrice();
//	                    boolean numeric = Common.isNumeric(stockPrice);
//	                    if (!numeric) {
//	                        stockPrice = "0.00";
//	                    }
//	                    oldDj = new BigDecimal(stockPrice);
//	                    zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
//	                } else {
//	                    oldDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
//	                    zj = qcnum.multiply(oldDj).setScale(2, BigDecimal.ROUND_HALF_UP);
//	                }
//	            }
//	        }
//
//	        // 期初数据存入map（保持原有正确数据）
//	        outmap.put("oldprice", oldDj);
//	        outmap.put("oldinventory", qcnum);
//	        outmap.put("oldpriceall", zj);
//
//	        // 计算入库/出库数量
//	        long in = 0;
//	        long out = 0;
//	        for (StockStatistics st : entry.getValue()) {
//	            if (st.isInOrOut()) {
//	                in += st.getNum();
//	            } else {
//	                out += st.getNum();
//	            }
//	        }
//
//	        // ====================== 重构变动单价逻辑（核心修改） ======================
//	        BigDecimal changeDj = BigDecimal.ZERO; // 入库/期末使用的变动单价
//	        if (isCurrentMonth) {
//	            // 当月：获取当月平均单价
//	            changeDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
//	        } else {
//	            // 历史月份：从已构建的Map中获取查询月份的月末单价（复用原有查询逻辑）
//	            MonthEndStatistics historyMonthStats = currentQueryMonthStatsMap.get(gs.getStock().getId());
//	            
//	            if (historyMonthStats != null && Common.isNotEmpty(historyMonthStats.getPrice()) 
//	                    && Common.isNumeric(historyMonthStats.getPrice())) {
//	                // 使用历史月份月末单价
//	                changeDj = new BigDecimal(historyMonthStats.getPrice());
//	            } else if (oldDj.compareTo(BigDecimal.ZERO) != 0) {
//	                // 降级使用期初单价
//	                changeDj = oldDj;
//	            } else if (Common.isNotEmpty(gs.getStock().getPrice()) && Common.isNumeric(gs.getStock().getPrice())) {
//	                // 最终降级使用Stock中的价格
//	                changeDj = new BigDecimal(gs.getStock().getPrice());
//	            } else {
//	                // 兜底计算平均单价
//	                changeDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH,"");
//	            }
//	        }
//
//	        // 计算入库金额
//	        BigDecimal inBig = new BigDecimal(in);
//	        BigDecimal inpriceall = BigDecimal.ZERO;
//	        if (in > 0) {
//	            inpriceall = inBig.multiply(changeDj).setScale(2, BigDecimal.ROUND_HALF_UP);
//	        }
//	        outmap.put("in", in);
//	        outmap.put("inprice", changeDj);
//	        outmap.put("inpriceall", inpriceall);
//
//	        // 计算出库单价：(期初总金额 + 入库总金额) / (期初数量 + 入库数量)
//	        BigDecimal outprice = BigDecimal.ZERO;
//	        BigDecimal denominator = qcnum.add(inBig); // 分母：期初数量 + 入库数量
//	        
//	        if(isCurrentMonth) {
//	        	  if (denominator.compareTo(BigDecimal.ZERO) > 0) { // 防止除数为0
//	  	            outprice = zj.add(inpriceall).divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
//	  	        } else if (changeDj.compareTo(BigDecimal.ZERO) > 0) { // 降级使用变动单价
//	  	            outprice = changeDj;
//	  	        } else {
//	  	            outprice = oldDj; // 最终降级使用期初单价
//	  	        }
//	        }else {
//	        	outprice = changeDj;
//	        }
//	      
//
//	        // 计算出库金额
//	        BigDecimal outBig = new BigDecimal(out);
//	        BigDecimal outpriceall = outBig.multiply(outprice).setScale(2, BigDecimal.ROUND_HALF_UP);
//	        outmap.put("out", out);
//	        outmap.put("outprice", outprice);
//	        outmap.put("outpriceall", outpriceall);
//
//	        // 计算期末库存数量（原有逻辑保留）
//	        BigDecimal qmnum = qcnum.add(inBig).subtract(outBig);
//	        
//	        // ========== 核心修改：期末总价计算逻辑 ==========
//	        // 期末总价 = 期初总金额 + 入库总金额 - 出库总金额
////	        BigDecimal qmzj = zj.add(inpriceall).subtract(outpriceall).setScale(2, BigDecimal.ROUND_HALF_UP);
//	        // 期末总价 = 期末数量 × 出库单价（保留2位小数，不四舍五入）
//	        BigDecimal qmzj = qmnum.multiply(outprice).setScale(2, BigDecimal.ROUND_DOWN);
//	        
//	        outmap.put("newprice", outprice);
//	        outmap.put("newinventory", qmnum);
//	        outmap.put("newpriceall", qmzj); // 存入修改后的期末总价
//	        outmap.put("purchaseInvoiceDate",
//	                Common.isEmpty(gs.getPurchaseInvoiceDate()) ? "" : gs.getPurchaseInvoiceDate());
//	        outlist.add(outmap);
//	    }
//
//	    // 导出Excel
//	    Map<String, Object> dataMap = new HashMap<>();
//	    dataMap.put("inlist", outlist);
//	    dataMap.put("title", requestBo.getStart() + "~" + requestBo.getEnd() + "库存统计");
//
//	    String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
//	    String fileName = "新库存统计导出模板.xlsx";
//	    TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
//	    Workbook doc = null;
//
//	    try {
//	        doc = ExcelExportUtil.exportExcel(params, dataMap);
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    }
//
//	    return doc;
//	}
//2026年3月12日14:25:18
//	public Workbook newExport2(HttpServletRequest request, RequestBo requestBo) {
//		Query query = new Query();
//		String end = "";
//		boolean isCurrentMonth = false; // 【新增】标记是否为当月查询
//		if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
//			end = requestBo.getEnd() + " 23:59:59";
//			isCurrentMonth = Common.isDateTimeInCurrentMonth(end); // 【新增】判断是否为当月
//
//			Criteria ca = new Criteria();
//			ca.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
//					Criteria.where("depotTime").gte(requestBo.getStart()).lte(end));
//			query.addCriteria(ca);
//		}
//
//		query.addCriteria(Criteria.where("revoke").is(false));
//		query.addCriteria(Criteria.where("byRevoke").is(false));
//		List<StockStatistics> list = this.find(query, StockStatistics.class);
//		List<Map<String, Object>> outlist = new ArrayList<>();
//
//		// 查询历史月度统计数据
//		String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(requestBo.getStart());
//		List<MonthEndStatistics> findMonthEndStatisticsByDate = this.monthEndStatisticsService
//				.findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
//		Map<String, MonthEndStatistics> listMonthEndStatisticsToMap = listMonthEndStatisticsToMap(
//				findMonthEndStatisticsByDate);
//
//		// 获取所有库存设备
//		Map<String, List<StockStatistics>> map = new HashMap<String, List<StockStatistics>>();
//		List<Stock> findAllStock = new ArrayList<Stock>();
//		if (Common.isDateTimeInCurrentMonth(end)) {
//			findAllStock = this.stockService.findAllStock();
//			List<StockStatistics> list2 = findAllStock.stream().map(stock -> {
//				StockStatistics st = new StockStatistics();
//				st.setNewNum(0.0);
//				st.setNum(0.0);
//				st.setStock(stock);
//				return st;
//			}).collect(Collectors.toList());
//			list.addAll(list2);
//		} else {
//			findAllStock = this.stockService.findAllStock();
//			List<StockStatistics> list2 = findMonthEndStatisticsByDate.stream().map(stock -> {
//				StockStatistics st = new StockStatistics();
//				st.setNewNum(stock.getMonthEndStockNum());
//				st.setNum(0.0);
//				st.setStock(stock.getStock());
//				return st;
//			}).collect(Collectors.toList());
//			list.addAll(list2);
//		}
//
//		// 构建设备库存统计Map
//		for (StockStatistics st : list) {
//			String stockId = st.getStock().getId();
//			boolean containsKey = map.containsKey(stockId);
//			if (containsKey) {
//				List<StockStatistics> mlist = map.get(stockId);
//				mlist.add(st);
//			} else {
//				List<StockStatistics> ls = new ArrayList<StockStatistics>();
//				ls.add(st);
//				map.put(stockId, ls);
//			}
//		}
//
//		// 遍历处理每个设备的统计数据
//		for (Map.Entry<String, List<StockStatistics>> entry : map.entrySet()) {
//			Map<String, Object> outmap = new HashMap<>();
//			StockStatistics gs = entry.getValue().get(entry.getValue().size() - 1);
//
//			// 设备基础信息赋值
//			outmap.put("area", Common.isEmpty(gs.getStock().getArea()) ? "" : gs.getStock().getArea().getName());
//			outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
//			outmap.put("modelName", Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
//			outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
//			if (Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
//				outmap.put("goodsStorage",
//						Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())
//								? "/" + gs.getStock().getGoodsStorage().getShelflevel()
//								: "");
//			}
//			outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent() ? "是" : "否");
//			outmap.put("unit", Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());
//
//			// 初始化变量
//			BigDecimal dj = new BigDecimal(0);// 单价
//			BigDecimal zj = new BigDecimal(0);// 总金额
//			BigDecimal newNum = new BigDecimal(gs.getNewNum());
//			BigDecimal num = new BigDecimal(gs.getNum());
//			BigDecimal qcnum = new BigDecimal(0);
//			MonthEndStatistics monthEndStatistics = listMonthEndStatisticsToMap.get(gs.getStock().getId());
//			if (Common.isNotEmpty(monthEndStatistics)) {
//				qcnum = new BigDecimal(monthEndStatistics.getMonthEndStockNum());
//			}
//
//			// ====================== 核心修改：非当月单价降级逻辑 ======================
//			if (isCurrentMonth) {
//				// 当月：维持原有逻辑
//				if (Common.isNotEmpty(gs.getStock().getPrice())) {
//					String price = gs.getStock().getPrice();
//					boolean numeric = Common.isNumeric(price);
//					if (!numeric) {
//						price = "0.00";
//					}
//					dj = new BigDecimal(price);
//					zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//				} else {
//					dj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH);
//					zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//				}
//			} else {
//				// 非当月：优先历史价格，零值/空值则降级到Stock
//				boolean useHistoryPrice = false;
//				if (Common.isNotEmpty(monthEndStatistics) && Common.isNotEmpty(monthEndStatistics.getPrice())) {
//					String historyPrice = monthEndStatistics.getPrice();
//					// 校验历史价格有效性 + 判断是否为0/0.00
//					boolean isValidPrice = Common.isNumeric(historyPrice);
//					BigDecimal historyPriceBig = isValidPrice ? new BigDecimal(historyPrice) : new BigDecimal("0.00");
//					// 仅当历史价格有效且≠0时使用
//					if (isValidPrice && historyPriceBig.compareTo(BigDecimal.ZERO) != 0) {
//						dj = historyPriceBig;
//						zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//						useHistoryPrice = true;
//					}
//				}
//				// 降级逻辑：历史价格为0/空/无效时，从Stock获取
//				if (!useHistoryPrice) {
//					if (Common.isNotEmpty(gs.getStock().getPrice())) {
//						String stockPrice = gs.getStock().getPrice();
//						boolean numeric = Common.isNumeric(stockPrice);
//						if (!numeric) {
//							stockPrice = "0.00";
//						}
//						dj = new BigDecimal(stockPrice);
//						zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//					} else {
//						dj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH);
//						zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//					}
//				}
//			}
//			// ====================== 核心修改结束 ======================
//
//			outmap.put("oldprice", dj);
//			outmap.put("oldinventory", qcnum);
//			outmap.put("oldpriceall", zj);
//
//			// 计算入库/出库数量
//			long in = 0;
//			long out = 0;
//			for (StockStatistics st : entry.getValue()) {
//				if (st.isInOrOut()) {
//					in += st.getNum();
//				} else {
//					out += st.getNum();
//				}
//			}
//
//			// 【修改】入库/出库单价：非当月沿用最终的dj（已处理降级逻辑）
//			if (isCurrentMonth) {
//				dj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH);
//			} else {
//				// 非当月无需额外兜底，dj已通过降级逻辑处理
//				if (dj.compareTo(BigDecimal.ZERO) == 0) {
//					// 最终兜底：若dj仍为0，从Stock获取
//					if (Common.isNotEmpty(gs.getStock().getPrice())) {
//						String stockPrice = gs.getStock().getPrice();
//						dj = Common.isNumeric(stockPrice) ? new BigDecimal(stockPrice) : new BigDecimal("0.00");
//					} else {
//						dj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH);
//					}
//				}
//			}
//
//			// 计算入库/出库金额
//			BigDecimal e = new BigDecimal(in);
//			if (in > 0) {
//				BigDecimal inpriceall = e.multiply(dj);
//				outmap.put("inpriceall", inpriceall);
//			} else {
//				outmap.put("inpriceall", 0);
//			}
//			outmap.put("in", in);
//			outmap.put("inprice", dj);
//
//			BigDecimal d = new BigDecimal(out);
//			BigDecimal outpriceall = d.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//			outmap.put("out", out);
//			outmap.put("outprice", dj);
//			outmap.put("outpriceall", outpriceall);
//
//			// 计算期末库存
//			StockStatistics gsend = entry.getValue().get(0);
//			BigDecimal qmnum = qcnum.add(e).subtract(d);
//			BigDecimal qmzj = dj.multiply(qmnum).setScale(2, BigDecimal.ROUND_HALF_UP);
//			outmap.put("newprice", dj);
//			outmap.put("newinventory", qmnum);
//			outmap.put("newpriceall", qmzj);
//			outmap.put("purchaseInvoiceDate",
//					Common.isEmpty(gs.getPurchaseInvoiceDate()) ? "" : gs.getPurchaseInvoiceDate());
//			outlist.add(outmap);
//		}
//
//		// 导出Excel
//		Map<String, Object> dataMap = new HashMap<>();
//		dataMap.put("inlist", outlist);
//		dataMap.put("title", requestBo.getStart() + "~" + requestBo.getEnd() + "库存统计");
//
//		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
//		String fileName = "新库存统计导出模板.xlsx";
//		TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
//		Workbook doc = null;
//
//		try {
//			doc = ExcelExportUtil.exportExcel(params, dataMap);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return doc;
//	}
//	public Workbook newExport2(HttpServletRequest request, RequestBo requestBo) {
//		// Query query=newQueryByRequestBo(requestBo);
//		Query query = new Query();
//		String end= "";
//		if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
//
////			String end = requestBo.getEnd();
//			end = requestBo.getEnd() + " 23:59:59";
//
//			Criteria ca = new Criteria();
//			ca.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
//					Criteria.where("depotTime").gte(requestBo.getStart()).lte(end));
//			query.addCriteria(ca);
//
//		}
//
//		query.addCriteria(Criteria.where("revoke").is(false));
//		query.addCriteria(Criteria.where("byRevoke").is(false));
//		List<StockStatistics> list = this.find(query, StockStatistics.class);
//		List<Map<String, Object>> outlist = new ArrayList<>();
//
//		// 根据当前查询的开始日期 获取上月的最后一天年月日
//		String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(requestBo.getStart());
//
//		List<MonthEndStatistics> findMonthEndStatisticsByDate = this.monthEndStatisticsService
//				.findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
//
//		Map<String, MonthEndStatistics> listMonthEndStatisticsToMap = listMonthEndStatisticsToMap(
//				findMonthEndStatisticsByDate);
//
//		// 获取 start时间 （第一天）库存的初始数量 出库数量+剩余库存数量 num+newNum
//		// 获取所有的设备
//		Map<String, List<StockStatistics>> map = new HashMap<String, List<StockStatistics>>();
//
//		// 获取到所有库存的设备信息
//		//判断查询的是当月还是前面月份的 如果是当月的直接查询所有库存，如果是查询之前月份的直接调用查询月末记录数据
//		List<Stock> findAllStock = new ArrayList<Stock>();
//		if(Common.isDateTimeInCurrentMonth(end)) {
//			findAllStock = this.stockService.findAllStock();
//			List<StockStatistics> list2 = findAllStock.stream().map(stock -> {
//				StockStatistics st = new StockStatistics();
//				st.setNewNum(0.0);
//				st.setNum(0.0);
//				st.setStock(stock);
//				return st;
//			}).collect(Collectors.toList());
//
//			list.addAll(list2);
//		}else {
//			findAllStock = this.stockService.findAllStock();
//			List<StockStatistics> list2 = findMonthEndStatisticsByDate.stream().map(stock -> {
//				StockStatistics st = new StockStatistics();
//				st.setNewNum(stock.getMonthEndStockNum());
//				st.setNum(0.0);
//				st.setStock(stock.getStock());
//				return st;
//			}).collect(Collectors.toList());
//
//			list.addAll(list2);
//		}
//		
//		
//		
//		
////		List<StockStatistics> list2 = findAllStock.stream().map(stock -> {
////			StockStatistics st = new StockStatistics();
////			st.setNewNum(0);
////			st.setNum(0);
////			st.setStock(stock);
////			return st;
////		}).collect(Collectors.toList());
////
////		list.addAll(list2);
//
//		// 在库存统计中获取遍历所有设备
//		for (StockStatistics st : list) {
//			String stockId = st.getStock().getId();
//			boolean containsKey = map.containsKey(stockId);
//			if (containsKey) {
//				List<StockStatistics> mlist = map.get(stockId);
//				mlist.add(st);
//			} else {
//				List<StockStatistics> ls = new ArrayList<StockStatistics>();
//				ls.add(st);
//				map.put(stockId, ls);
//			}
//		}
//
//		// 集合的最后一条数据就是起初日期
//		// 遍历map ，将map数据放到outlist中
//
//		for (Map.Entry<String, List<StockStatistics>> entry : map.entrySet()) {
//
//			Map<String, Object> outmap = new HashMap<>();// 定义输出到excel的map
//
//			// 期初库存 获取集合的最后一条数据为第一条记录
//
//			StockStatistics gs = entry.getValue().get(entry.getValue().size() - 1);
//			// 获取设备其他信息
//			outmap.put("area", Common.isEmpty(gs.getStock().getArea()) ? "" : gs.getStock().getArea().getName());
//			outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
//			outmap.put("modelName", Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
//			outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
//			if (Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
//				outmap.put("goodsStorage",
//						Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())
//								? "/" + gs.getStock().getGoodsStorage().getShelflevel()
//								: "");
//			}
//
//			outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent() ? "是" : "否");
//			outmap.put("unit", Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());
//
////			BigDecimal qcnum ;//期初库存 为当前时间的前一次库存数量  // new 包含本期未出入库，但有期末余额的库存，数量取期末余额。
//			BigDecimal dj = new BigDecimal(0);// 单价
//			BigDecimal zj = new BigDecimal(0);// 总金额
//			BigDecimal newNum = new BigDecimal(gs.getNewNum());
//			BigDecimal num = new BigDecimal(gs.getNum());
////			if(gs.isInOrOut()) {
////				//如果是入库，需要减去入库数量
////				qcnum =  newNum.subtract(num);
////			}else if(!gs.isInOrOut()){
////				//如果是出库 需要吧出库数量加回去
////				qcnum = newNum.add(num) ;//获得期初库存数量
////			}else {
////				qcnum = newNum;
////			}
//			BigDecimal qcnum = new BigDecimal(0);
//			// 从定时任务统计的数据中拿到期初库存数据
//			MonthEndStatistics monthEndStatistics = listMonthEndStatisticsToMap.get(gs.getStock().getId());
//			if (Common.isNotEmpty(monthEndStatistics)) {
//				qcnum = new BigDecimal(monthEndStatistics.getMonthEndStockNum());
//			}
//
//			if (Common.isNotEmpty(gs.getStock().getPrice())) {
//				String price = gs.getStock().getPrice();
//				//根据stock的id去统计路面拿单价
//				boolean numeric = Common.isNumeric(price);
//				if (!numeric) {
//					price = "0.00";
//				}
//				dj = new BigDecimal(price);// 期初单价
//				zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);// 出库总额
//			}else {
//				dj = this.calculateAveragePriceByStockId(gs.getStock().getId(),TimeRangeType.CURRENT_MONTH);
//				zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);// 出库总额
//			}
//
//				
//				
//			outmap.put("oldprice", dj);
//			outmap.put("oldinventory", qcnum);
//			outmap.put("oldpriceall", zj);
//
////			//20221123 添加出库入库总价
////			private Double inprice;
//
//			long in = 0;// 入库数量
//			// long inpriceall =0;//入库总价
//			long out = 0;// 出库数量
////			int insize =0; //获入库次数
////			int outsize =0;//获取出库次数
//
//			for (StockStatistics st : entry.getValue()) {
//
//				if (st.isInOrOut()) {
//					in += st.getNum();// 入库总数
////					insize++;//入库量计数
//					// inpriceall+=Common.isNotEmpty(st.getInprice())?st.getInprice():0;//所有入库总额
//				} else {
//					out += st.getNum();// 出库总数
////					outsize++;//出库量计数
//
//				}
//			}
//
////			BigDecimal a = new BigDecimal(inpriceall);
//			//获取最新平均单价来生成本月入库 
//			dj = this.calculateAveragePriceByStockId(gs.getStock().getId(),TimeRangeType.CURRENT_MONTH);
//			BigDecimal a = dj;
//			BigDecimal b = new BigDecimal(in);
//
////			BigDecimal crkdj = new BigDecimal(0);
////			if(in>0) {
////				crkdj =  a.divide(b,2,BigDecimal.ROUND_HALF_UP);
////			}
//
//			BigDecimal e = new BigDecimal(in);
//			if (in > 0) {
//				BigDecimal inpriceall = e.multiply(dj);
//				outmap.put("inpriceall", inpriceall);// 入库总额
//			} else {
//				outmap.put("inpriceall", 0);// 入库总额
//			}
//
//			outmap.put("in", in);// 入库数量
//			outmap.put("inprice", dj);// 入库单价=所有入库总额/入库数量
//
//			BigDecimal d = new BigDecimal(out);
//			BigDecimal outpriceall = d.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);// 出库总额
//			outmap.put("out", out);// 出库数量
//			outmap.put("outprice", dj);// 出库单价=所有入库总额/入库数量
//			outmap.put("outpriceall", outpriceall);// 出库总额
//
//			// 期末库存数量 单价 金额
//			StockStatistics gsend = entry.getValue().get(0);
////			long qmnum =0;//期初库存
////			long qmdj =0;//单价
////			long qmzj =0;//总金额
//
//			// qmnum = gsend.getNewNum();//期末库存数量
//			// 期初库存+入库-出库
//			BigDecimal qmnum = qcnum.add(e).subtract(d);
//
//			//期末总价  期初库存+入库数量-出库数量
//	
//			BigDecimal qmzj = dj.multiply(qmnum).setScale(2, BigDecimal.ROUND_HALF_UP);
//			
//			
////			BigDecimal qmzj =zj.add(a).subtract(outpriceall);//期末总价
////			BigDecimal qmdj = new BigDecimal(0);
////			if(qmnum>0) {
////				 qmdj =  qmzj.divide(new BigDecimal(qmnum),2,BigDecimal.ROUND_HALF_UP);
////			}
//			outmap.put("newprice", dj);
//			outmap.put("newinventory", qmnum);
//			outmap.put("newpriceall", qmzj);
//			outmap.put("purchaseInvoiceDate",
//					Common.isEmpty(gs.getPurchaseInvoiceDate()) ? "" : gs.getPurchaseInvoiceDate());
//			outlist.add(outmap);
//
//		}
//
//		Map<String, Object> dataMap = new HashMap<>();
//
//		dataMap.put("inlist", outlist);
//		dataMap.put("title", requestBo.getStart() + "~" + requestBo.getEnd() + "库存统计");
//
//		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
//		String fileName = "新库存统计导出模板.xlsx";
//		TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
//		Workbook doc = null;
//
//		try {
//			doc = ExcelExportUtil.exportExcel(params, dataMap);
////							WordUtil.exportWord(ctxPath+fileName, dataMap);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return doc;
//
//	}

//	@Override
//	public Workbook newExport3(HttpServletRequest request, String search, String start, String end, String type,
//			String name, String areaId, String searchAgent) {
//
//		List<StockStatistics> list1 = this.findStockStatistics(search, start, end, type, areaId, searchAgent);
//
//		Map<String, String> findDateByInputDate = Common.findDateByInputDate(start);
//		String ostart = findDateByInputDate.get("startDate");
//		String oend = findDateByInputDate.get("endDate");
//
//		List<StockStatistics> list2 = this.findStockStatistics(search, ostart, oend, type, areaId, searchAgent);
//		List<StockStatistics> list = list1.stream().filter(item -> list2.stream().map(e -> e.getStock().getId())
//				.collect(Collectors.toList()).contains(item.getStock().getId())).collect(Collectors.toList());
//
//// 【新增】当月判断 + 历史数据查询
//		String endWithTime = Common.isNotEmpty(end) ? end + " 23:59:59" : "";
//		boolean isCurrentMonth = Common.isNotEmpty(endWithTime) ? Common.isDateTimeInCurrentMonth(endWithTime) : false;
//		String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(start);
//		List<MonthEndStatistics> findMonthEndStatisticsByDate = this.monthEndStatisticsService
//				.findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
//		Map<String, MonthEndStatistics> monthEndStatisticsMap = new HashMap<>();
//		if (Common.isNotEmpty(findMonthEndStatisticsByDate)) {
//			for (MonthEndStatistics mes : findMonthEndStatisticsByDate) {
//				if (Common.isNotEmpty(mes.getStock()) && Common.isNotEmpty(mes.getStock().getId())) {
//					monthEndStatisticsMap.put(mes.getStock().getId(), mes);
//				}
//			}
//		}
//
//		List<Map<String, Object>> outlist = new ArrayList<>();
//		Map<String, List<StockStatistics>> map = new HashMap<String, List<StockStatistics>>();
//		for (StockStatistics st : list) {
//			String stockId = st.getStock().getId();
//			boolean containsKey = map.containsKey(stockId);
//			if (containsKey) {
//				List<StockStatistics> mlist = map.get(stockId);
//				mlist.add(st);
//			} else {
//				List<StockStatistics> ls = new ArrayList<StockStatistics>();
//				ls.add(st);
//				map.put(stockId, ls);
//			}
//		}
//
//		for (Map.Entry<String, List<StockStatistics>> entry : map.entrySet()) {
//			Map<String, Object> outmap = new HashMap<>();
//			StockStatistics gs = entry.getValue().get(entry.getValue().size() - 1);
//
//// 设备基础信息
//			outmap.put("area", Common.isEmpty(gs.getArea()) ? "" : gs.getArea().getName());
//			outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
//			outmap.put("modelName", Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
//			outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
//			if (Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
//				outmap.put("goodsStorage",
//						Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())
//								? "/" + gs.getStock().getGoodsStorage().getShelflevel()
//								: "");
//			}
//			outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent() ? "是" : "否");
//			outmap.put("unit", Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());
//
//// 期初库存数量计算
//			BigDecimal qcnum;
//			BigDecimal dj = new BigDecimal(0);
//			BigDecimal zj = new BigDecimal(0);
//			BigDecimal newNum = new BigDecimal(gs.getNewNum());
//			BigDecimal num = new BigDecimal(gs.getNum());
//			if (gs.isInOrOut()) {
//				qcnum = newNum.subtract(num);
//			} else {
//				qcnum = newNum.add(num);
//			}
//
//// ====================== 核心修改：非当月单价降级逻辑 ======================
//			MonthEndStatistics monthEndStatistics = monthEndStatisticsMap.get(gs.getStock().getId());
//			if (isCurrentMonth) {
//// 当月：原逻辑
//				if (Common.isNotEmpty(gs.getStock().getPrice())) {
//					String price = gs.getStock().getPrice();
//					boolean numeric = Common.isNumeric(price);
//					if (!numeric) {
//						price = "0.00";
//					}
//					dj = new BigDecimal(price);
//					zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//				} else {
//					dj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH);
//					zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//				}
//			} else {
//// 非当月：优先历史价格，失败则降级到Stock
//				boolean useHistoryPrice = false;
//				if (Common.isNotEmpty(monthEndStatistics) && Common.isNotEmpty(monthEndStatistics.getPrice())) {
//					String historyPrice = monthEndStatistics.getPrice();
//					boolean isValidPrice = Common.isNumeric(historyPrice);
//					BigDecimal historyPriceBig = isValidPrice ? new BigDecimal(historyPrice) : new BigDecimal("0.00");
//					// 历史价格有效且≠0时使用
//					if (isValidPrice && historyPriceBig.compareTo(BigDecimal.ZERO) != 0) {
//						dj = historyPriceBig;
//						zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//						useHistoryPrice = true;
//					}
//				}
//// 降级：从Stock获取
//				if (!useHistoryPrice) {
//					if (Common.isNotEmpty(gs.getStock().getPrice())) {
//						String stockPrice = gs.getStock().getPrice();
//						boolean numeric = Common.isNumeric(stockPrice);
//						if (!numeric) {
//							stockPrice = "0.00";
//						}
//						dj = new BigDecimal(stockPrice);
//						zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//					} else {
//						dj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH);
//						zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);
//					}
//				}
//			}
//// ====================== 核心修改结束 ======================
//
//			outmap.put("oldprice", dj);
//			outmap.put("oldinventory", qcnum);
//			outmap.put("oldpriceall", zj);
//
//// 入库/出库数量&金额计算
//			long in = 0;
//			long inpriceall = 0;
//			long out = 0;
//			for (StockStatistics st : entry.getValue()) {
//				if (st.isInOrOut()) {
//					in += st.getNum();
//					inpriceall += Common.isNotEmpty(st.getInprice()) ? st.getInprice() : 0;
//				} else {
//					out += st.getNum();
//				}
//			}
//
//			BigDecimal a = new BigDecimal(inpriceall);
//			BigDecimal b = new BigDecimal(in);
//			BigDecimal crkdj = new BigDecimal(0);
//			if (in > 0) {
//				crkdj = a.divide(b, 2, BigDecimal.ROUND_HALF_UP);
//			}
//
//// 非当月沿用历史单价（dj）
//			BigDecimal finalCrkdj = crkdj;
//			if (!isCurrentMonth && dj.compareTo(BigDecimal.ZERO) != 0) {
//				finalCrkdj = dj;
//			}
//
//			outmap.put("in", in);
//			outmap.put("inprice", finalCrkdj);
//			outmap.put("inpriceall", inpriceall);
//
//			BigDecimal d = new BigDecimal(out);
//			BigDecimal outpriceall = d.multiply(finalCrkdj).setScale(2, BigDecimal.ROUND_HALF_UP);
//			outmap.put("out", out);
//			outmap.put("outprice", finalCrkdj);
//			outmap.put("outpriceall", outpriceall);
//
//// 期末库存计算
//			StockStatistics gsend = entry.getValue().get(0);
//			Double qmnum = gsend.getNewNum();
//			BigDecimal qmzj = zj.add(a).subtract(outpriceall);
//			BigDecimal qmdj = new BigDecimal(0);
//			if (qmnum > 0) {
//				qmdj = qmzj.divide(new BigDecimal(qmnum), 2, BigDecimal.ROUND_HALF_UP);
//			}
//			outmap.put("newprice", qmdj);
//			outmap.put("newinventory", qmnum);
//			outmap.put("newpriceall", qmzj);
//			outmap.put("purchaseInvoiceDate",
//					Common.isEmpty(gs.getPurchaseInvoiceDate()) ? "" : gs.getPurchaseInvoiceDate());
//			outlist.add(outmap);
//		}
//
//		Map<String, Object> dataMap = new HashMap<>();
//		dataMap.put("inlist", outlist);
//		dataMap.put("title", start + "~" + end + "库存统计");
//
//		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
//		String fileName = "新库存统计导出模板.xlsx";
//		TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
//		Workbook doc = null;
//
//		try {
//			doc = ExcelExportUtil.exportExcel(params, dataMap);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return doc;
//	}
	@Override
	public Workbook newExport3(HttpServletRequest request, String search, String start, String end, String type,
			String name, String areaId, String searchAgent) {

		// List<Stock> listStock = this.findStocksBySearch(search, areaId, searchAgent);
		// 获取选择月份的统计数据
		List<StockStatistics> list1 = this.findStockStatistics(search, start, end, type, areaId, searchAgent);

		Map<String, String> findDateByInputDate = Common.findDateByInputDate(start);
		String ostart = findDateByInputDate.get("startDate");
		String oend = findDateByInputDate.get("endDate");

		List<StockStatistics> list2 = this.findStockStatistics(search, ostart, oend, type, areaId, searchAgent);
		List<StockStatistics> list = list1.stream().filter(item -> list2.stream().map(e -> e.getStock().getId())
				.collect(Collectors.toList()).contains(item.getStock().getId())).collect(Collectors.toList());

		// 获取所有的库存

		List<Map<String, Object>> outlist = new ArrayList<>();
		// 获取 start时间 （第一天）库存的初始数量 出库数量+剩余库存数量 num+newNum
		// 获取所有的设备
		Map<String, List<StockStatistics>> map = new HashMap<String, List<StockStatistics>>();
		// 在库存统计中获取遍历所有设备
		for (StockStatistics st : list) {
			String stockId = st.getStock().getId();
			boolean containsKey = map.containsKey(stockId);
			if (containsKey) {
				List<StockStatistics> mlist = map.get(stockId);
				mlist.add(st);
			} else {
				List<StockStatistics> ls = new ArrayList<StockStatistics>();
				ls.add(st);
				map.put(stockId, ls);
			}
		}
		// 集合的最后一条数据就是起初日期
		// 遍历map ，将map数据放到outlist中

		for (Map.Entry<String, List<StockStatistics>> entry : map.entrySet()) {

			Map<String, Object> outmap = new HashMap<>();// 定义输出到excel的map

			// 期初库存 获取集合的最后一条数据为第一条记录

			StockStatistics gs = entry.getValue().get(entry.getValue().size() - 1);
			// 获取设备其他信息
			outmap.put("area", Common.isEmpty(gs.getArea()) ? "" : gs.getArea().getName());
			outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
			outmap.put("modelName", Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
			outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
			if (Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
				outmap.put("goodsStorage",
						Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())
								? "/" + gs.getStock().getGoodsStorage().getShelflevel()
								: "");
			}

			outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent() ? "是" : "否");
			outmap.put("unit", Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());

			BigDecimal qcnum;// 期初库存 为当前时间的前一次库存数量
			BigDecimal dj = new BigDecimal(0);// 单价
			BigDecimal zj = new BigDecimal(0);// 总金额
			BigDecimal newNum = new BigDecimal(gs.getNewNum());
			BigDecimal num = new BigDecimal(gs.getNum());
			if (gs.isInOrOut()) {
				// 如果是入库，需要减去入库数量
				qcnum = newNum.subtract(num);
			} else {
				// 如果是出库 需要吧出库数量加回去
				qcnum = newNum.add(num);// 获得期初库存数量
			}
			
			
			if (Common.isNotEmpty(gs.getStock().getPrice())) {
				String price = gs.getStock().getPrice();
				boolean numeric = Common.isNumeric(price);
				if (!numeric) {
					price = "0.00";
				}
				dj = new BigDecimal(price);// 期初单价
				zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);// 出库总额
			}else {
				dj = this.calculateAveragePriceByStockId(gs.getStock().getId(),TimeRangeType.CURRENT_MONTH,"");
				zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);// 出库总额
			}
			
			
//			dj = this.calculateAveragePriceByStockId(gs.getStock().getId(),TimeRangeType.CURRENT_MONTH);
//			zj = qcnum.multiply(dj).setScale(2, BigDecimal.ROUND_HALF_UP);// 出库总额
			
			outmap.put("oldprice", dj);
			outmap.put("oldinventory", qcnum);
			outmap.put("oldpriceall", zj);

//			//20221123 添加出库入库总价
//			private Double inprice;

			long in = 0;// 入库数量
			long inpriceall = 0;// 入库总价
			long out = 0;// 出库数量
//			int insize =0; //获入库次数
//			int outsize =0;//获取出库次数

			for (StockStatistics st : entry.getValue()) {

				if (st.isInOrOut()) {
					in += st.getNum();// 入库总数
//					insize++;//入库量计数
					inpriceall += Common.isNotEmpty(st.getInprice()) ? st.getInprice() : 0;// 所有入库总额
				} else {
					out += st.getNum();// 出库总数
//					outsize++;//出库量计数

				}
			}

			BigDecimal a = new BigDecimal(inpriceall);
			BigDecimal b = new BigDecimal(in);

			BigDecimal crkdj = new BigDecimal(0);
			if (in > 0) {
				crkdj = a.divide(b, 2, BigDecimal.ROUND_HALF_UP);
			}

			outmap.put("in", in);// 入库数量
			outmap.put("inprice", crkdj);// 入库单价=所有入库总额/入库数量
			outmap.put("inpriceall", inpriceall);// 入库总额

			BigDecimal d = new BigDecimal(out);
//			BigDecimal e = new BigDecimal(in);
			BigDecimal outpriceall = d.multiply(crkdj).setScale(2, BigDecimal.ROUND_HALF_UP);// 出库总额
			outmap.put("out", out);// 出库数量
			outmap.put("outprice", crkdj);// 出库单价=所有入库总额/入库数量
			outmap.put("outpriceall", outpriceall);// 出库总额

			// 期末库存数量 单价 金额
			StockStatistics gsend = entry.getValue().get(0);
			Double qmnum = 0.0;// 期初库存
//			long qmdj =0;//单价
//			long qmzj =0;//总金额

			qmnum = gsend.getNewNum();// 期末库存数量
			BigDecimal qmzj = zj.add(a).subtract(outpriceall);// 期末总价
			BigDecimal qmdj = new BigDecimal(0);
			if (qmnum > 0) {
				qmdj = qmzj.divide(new BigDecimal(qmnum), 2, BigDecimal.ROUND_HALF_UP);
			}
			outmap.put("newprice", qmdj);
			outmap.put("newinventory", qmnum);
			outmap.put("newpriceall", qmzj);
			outmap.put("purchaseInvoiceDate",
					Common.isEmpty(gs.getPurchaseInvoiceDate()) ? "" : gs.getPurchaseInvoiceDate());
			outlist.add(outmap);

		}

		Map<String, Object> dataMap = new HashMap<>();

		dataMap.put("inlist", outlist);
		dataMap.put("title", start + "~" + end + "库存统计");

		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
		String fileName = "新库存统计导出模板.xlsx";
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

	public Query newQueryByRequestBo(RequestBo requestBo) {
		Query query = new Query();
		Criteria ca = new Criteria();

		if (Common.isNotEmpty(requestBo.getName()) || Common.isNotEmpty(requestBo.getSupplier())
				|| Common.isNotEmpty(requestBo.getPaymentOrderNo()) || Common.isNotEmpty(requestBo.getSearchArea())
				|| Common.isNotEmpty(requestBo.getModel())) {
			Query querys = new Query();
			querys = this.stockService.findByRequestBo(requestBo, querys);
//			querys.addCriteria(Criteria.where("isDelete").is(false));
			List<Stock> stocks = this.stockService.find(querys, Stock.class);
			List<Object> stockids = stocks.stream().map(stock -> new ObjectId(stock.getId()))
					.collect(Collectors.toList());
			query = query.addCriteria(Criteria.where("stock.$id").in(stockids));
		}
		if (Common.isNotEmpty(requestBo.getId())) {
			ca.orOperator(Criteria.where("stock.$id").is(new ObjectId(requestBo.getId())));
		}
		if (Common.isNotEmpty(requestBo.getUserId())) {
			ca.orOperator(Criteria.where("financeUser.$id").is(new ObjectId(requestBo.getUserId())));
		}
		if (Common.isNotEmpty(requestBo.getPname())) {
			ca.orOperator(Criteria.where("pname.$id").is(new ObjectId(requestBo.getPname())));
		}
		if (Common.isNotEmpty(requestBo.getItemNo())) {
			query = query.addCriteria(Criteria.where("newItemNo").regex(requestBo.getItemNo(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getPurchaseInvoiceNo())) {
			query = query.addCriteria(Criteria.where("purchaseInvoiceNo").regex(requestBo.getPurchaseInvoiceNo(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getPaymentOrderNo())) {
			query = query.addCriteria(Criteria.where("paymentOrderNo").regex(requestBo.getPaymentOrderNo(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getPurchaseInvoiceDate())) {
			query = query
					.addCriteria(Criteria.where("purchaseInvoiceDate").regex(requestBo.getPurchaseInvoiceDate(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getProjectName())) {
			query = query.addCriteria(Criteria.where("projectName").regex(requestBo.getProjectName(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getAccepter())) {
			query = query.addCriteria(Criteria.where("accepter").regex(requestBo.getAccepter(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getEntryName())) {
			String[] sas = requestBo.getEntryName().split(",");
			query = query.addCriteria(Criteria.where("pname.$id")
					.in(Arrays.stream(sas).map(str -> new ObjectId(str)).collect(Collectors.toList())));
		}
//		if(Common.isNotEmpty(requestBo.getCustomer())){
//			query=query.addCriteria(Criteria.where("customer").regex(requestBo.getCustomer(), "i"));
//		}
		if (Common.isNotEmpty(requestBo.getCustomer())) {
			Query squery = new Query();
			squery.addCriteria(Criteria.where("name").regex(requestBo.getCustomer(), "i"));
			List<NewCustomer> customerList = this.newCustomerService.find(squery, NewCustomer.class);
			if (!customerList.isEmpty()) {
				query = query.addCriteria(Criteria.where("newCustomer.$id").in(customerList.stream()
						.map(newCustomer -> new ObjectId(newCustomer.getId())).collect(Collectors.toList())));
//				ca.orOperator(Criteria.where("newCustomer.$id").in(customerList.stream()
//						.map(newCustomer -> new ObjectId(newCustomer.getId())).collect(Collectors.toList())));

			}
		}

		if (Common.isNotEmpty(requestBo.getConfirm())) {
			query = query.addCriteria(Criteria.where("confirm").is(Boolean.valueOf(requestBo.getConfirm())));
		}
		if (Common.isNotEmpty(requestBo.getSailesInvoiceNo())) {
			query = query.addCriteria(Criteria.where("sailesInvoiceNo").regex(requestBo.getSailesInvoiceNo(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getType())) {
			if (requestBo.getType().equals("in")) {
				query.addCriteria(Criteria.where("inOrOut").is(true));
			} else if (requestBo.getType().equals("out")) {
				query.addCriteria(Criteria.where("inOrOut").is(false));
			}
		}
		if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
			String end = requestBo.getEnd();
			end = end + " 23:59:59";
			Criteria ca1 = new Criteria();
			ca1.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
					Criteria.where("depotTime").gte(requestBo.getStart()).lte(end));
			ca.andOperator(ca1);
		}
		query.addCriteria(ca);
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		return query;
	}

	/**
	 * 将数据转换成 k v形式 取值的时候通过使用key直接获取
	 * 
	 * @param list
	 * @return
	 */
	public Map<String, MonthEndStatistics> listMonthEndStatisticsToMap(List<MonthEndStatistics> list) {

		Map<String, MonthEndStatistics> map = new HashMap<String, MonthEndStatistics>();
		list.forEach(m -> {
			map.put(m.getStock().getId(), m);
		});
		return map;

	}

	/**
	 * 通过 项目id 客户id 领料人 来查询数据
	 */
	@Override
	public List<StockStatistics> findStockStatisticsToCreateQrcode(Pname pname, NewCustomer newcustomer,
			String accepter, String depotTime) {

		Query query = new Query();
		query.addCriteria(Criteria.where("revoke").is(false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		if (Common.isNotEmpty(pname)) {
			query.addCriteria(Criteria.where("pname.$id").is(new ObjectId(pname.getId())));
		}
		if (Common.isNotEmpty(newcustomer)) {
			query.addCriteria(Criteria.where("newCustomer.$id").is(new ObjectId(newcustomer.getId())));
		}
		if (Common.isNotEmpty(accepter)) {
			query.addCriteria(Criteria.where("accepter").is(accepter));
		}
		if (Common.isNotEmpty(depotTime)) {
//			try {
//				String dateYMD = Common.getDateYMD(depotTime);
//				query.addCriteria(Criteria.where("depotTime").regex(dateYMD));
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			query.addCriteria(Criteria.where("depotTime").is(depotTime));

		}

		return this.find(query, StockStatistics.class);
	}

	public Workbook exportStockStatistics(HttpServletRequest request, String start, String end) {
		// List<Stock> listStock = this.findStocksBySearch(search, areaId, searchAgent);
		// 获取选择月份的统计数据
		List<StockStatistics> list = this.findStockStatisticsbySE(start, end);

		List<Map<String, Object>> inlist = new ArrayList<>();
//				项目名称	供应商	设备名称	品名型号	出库时间	出库数量	单价	     总价	    单位	    负责人	客户	    领料人	项目助理	操作人
//	{{$fe:inlist t.t1	t.t2	t.t3	t.t4	t.t5	t.t6	t.t7	t.t8	t.t9	t.t10	t.t11	t.t12	t.t13	t.t14}}

		for (StockStatistics st : list) {
			Map<String, Object> in = new HashMap<>();

			Double num = Common.isNotEmpty(st.getNum()) ? st.getNum() : 0.0;
			Double dj = Common.isNotEmpty(st.getStock().getPrice()) ? Double.parseDouble(st.getStock().getPrice())
					: 0.0;
			Double allprice = num * dj;
			in.put("t1", Common.isNotEmpty(st.getPname()) ? st.getPname().getName() : "");
			in.put("t2", Common.isNotEmpty(st.getStock().getSupplier()) ? st.getStock().getSupplier().getName() : "");
			in.put("t3", Common.isNotEmpty(st.getStock().getName()) ? st.getStock().getName() : "");
			in.put("t4", Common.isNotEmpty(st.getStock().getModel()) ? st.getStock().getModel() : "");
			in.put("t5", Common.isNotEmpty(st.getDepotTime()) ? st.getDepotTime() : "");
			in.put("t6", num);
			in.put("t7", dj);
			in.put("t8", allprice);
			in.put("t9", Common.isNotEmpty(st.getStock().getUnit()) ? st.getStock().getUnit().getName() : "");
			in.put("t10", Common.isNotEmpty(st.getPname().getPm()) ? st.getPname().getPm() : "");
			in.put("t11", Common.isNotEmpty(st.getNewCustomer()) ? st.getNewCustomer().getName() : "");
			in.put("t12", Common.isNotEmpty(st.getAccepter()) ? st.getAccepter() : "");
			in.put("t13", Common.isNotEmpty(st.getPname()) ? st.getPname().getAssistant() : "");
			in.put("t14", Common.isNotEmpty(st.getUser()) ? st.getUser().getUserName() : "");
			inlist.add(in);
		}

		Map<String, Object> dataMap = new HashMap<>();

		dataMap.put("inlist", inlist);
		dataMap.put("title", start + "~" + end + "项目统计");

		String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
		String fileName = "项目统计导出模板.xlsx";
		TemplateExportParams params = new TemplateExportParams(ctxPath + fileName, true);
		Workbook doc = null;

		try {
			doc = ExcelExportUtil.exportExcel(params, dataMap);
//									WordUtil.exportWord(ctxPath+fileName, dataMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return doc;

	}

	// 根据开始 结束时间查看出库统计
	private List<StockStatistics> findStockStatisticsbySE(String start, String end) {

		Query query = new Query();

		end = end + " 23:59:59";
		query.addCriteria(Criteria.where("revoke").is(false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		query.addCriteria(Criteria.where("inOrOut").is(false));
		query.addCriteria(Criteria.where("depotTime").gte(start).lte(end));

		return this.find(query, StockStatistics.class);

	}

//	public List<StockStatistics> findStockStatisByStockId(String id) {
//		Query query  = new Query();
//		query.addCriteria(Criteria.where("revoke").is(false));
//		query.addCriteria(Criteria.where("isDelete").is(false));
//		query.addCriteria(Criteria.where("isDisable").is(false));
//		query.addCriteria(Criteria.where("inOrOut").is(true));
//		query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(id)));
//		return this.find(query, StockStatistics.class);
//		
//	}
//	

//	 public List<StockStatistics> findStockStatisByStockId(String id) {
//	        Query query = new Query();
//	        // 原有过滤条件（保持不变）
//	        query.addCriteria(Criteria.where("revoke").is(false));
//	        query.addCriteria(Criteria.where("isDelete").is(false));
//	        query.addCriteria(Criteria.where("isDisable").is(false));
//	        query.addCriteria(Criteria.where("inOrOut").is(true));
//	        query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(id)));
//
//	        // 新增：过滤当前年份1月1日至今的数据
//	        // 1. 获取当前年份的1月1日 00:00:00 时间点
//	        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")); // 指定时区（避免时区偏差）
//	        calendar.set(Calendar.MONTH, Calendar.JANUARY); // 月份设为1月（Calendar.JANUARY=0）
//	        calendar.set(Calendar.DAY_OF_MONTH, 1); // 日期设为1号
//	        calendar.set(Calendar.HOUR_OF_DAY, 0); // 小时设为0
//	        calendar.set(Calendar.MINUTE, 0); // 分钟设为0
//	        calendar.set(Calendar.SECOND, 0); // 秒设为0
//	        calendar.set(Calendar.MILLISECOND, 0); // 毫秒设为0
//	        Date startOfYear = calendar.getTime(); // 当年1月1日 00:00:00
//
//	        // 2. 添加时间条件：数据的时间字段 >= 当年1月1日
//	        // 注意：将 "createTime" 替换为你 StockStatistics 实体中实际的时间字段名（如 updateTime/statisTime 等）
//	        query.addCriteria(Criteria.where("createTime").gte(startOfYear));
//
//	        return this.find(query, StockStatistics.class);
//	    }

	// 定义枚举类，明确查询范围类型（比字符串更规范，避免传参错误）
	public enum TimeRangeType {
		CURRENT_YEAR, // 当年1月1日至今
		CURRENT_MONTH // 当月1日至今
	}

	/**
	 * 根据stockId和时间范围查询数据
	 * 
	 * @param id            库存ID
	 * @param timeRangeType 时间范围类型（CURRENT_YEAR/CURRENT_MONTH）
	 * @return 符合条件的统计数据
	 */
	public List<StockStatistics> findStockStatisByStockId(String id, TimeRangeType timeRangeType, String date) {
	    Query query = new Query();
	    // 原有过滤条件（保持不变）
	    query.addCriteria(Criteria.where("revoke").is(false));
	    query.addCriteria(Criteria.where("isDelete").is(false));
	    query.addCriteria(Criteria.where("isDisable").is(false));
	    query.addCriteria(Criteria.where("inOrOut").is(true));
	    query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(id)));

	    // ========== 核心修改：根据date是否为空动态计算startTime ==========
	    Date startTime = null;
	    // 1. 判断date是否为空/Null
	    if (date == null || date.trim().isEmpty()) {
	        // date为空 → 使用timeRangeType计算起始时间
	        startTime = calculateStartTime(timeRangeType);
	    } else {
	        // date非空 → 先转成Date，再调整为当月1号0点0分0秒
	        try {
	            // 步骤1：解析日期字符串为Date（格式：yyyy-MM-dd）
	            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	            Date originalDate = sdf.parse(date.trim());
	            
	            // 步骤2：将日期调整为当月1号0点0分0秒
	            Calendar calendar = Calendar.getInstance();
	            calendar.setTime(originalDate);
	            calendar.set(Calendar.DAY_OF_MONTH, 1); // 设为当月1号
	            calendar.set(Calendar.HOUR_OF_DAY, 0);  // 小时设为0
	            calendar.set(Calendar.MINUTE, 0);       // 分钟设为0
	            calendar.set(Calendar.SECOND, 0);       // 秒设为0
	            calendar.set(Calendar.MILLISECOND, 0);  // 毫秒设为0
	            startTime = calendar.getTime();
	            
	        } catch (ParseException e) {
	            // 日期格式错误时，降级使用timeRangeType（避免方法报错）
	            System.err.println("日期格式错误（需为yyyy-MM-dd），降级使用时间范围：" + e.getMessage());
	            startTime = calculateStartTime(timeRangeType);
	        }
	    }

	    // 添加时间过滤条件（createTime >= startTime）
	    query.addCriteria(Criteria.where("createTime").gte(startTime));

	    return this.find(query, StockStatistics.class);
	}
//	public List<StockStatistics> findStockStatisByStockId(String id, TimeRangeType timeRangeType, String date) {
//	    Query query = new Query();
//	    // 原有过滤条件（保持不变）
//	    query.addCriteria(Criteria.where("revoke").is(false));
//	    query.addCriteria(Criteria.where("isDelete").is(false));
//	    query.addCriteria(Criteria.where("isDisable").is(false));
//	    query.addCriteria(Criteria.where("inOrOut").is(true));
//	    query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(id)));
//
//	    // ========== 核心修改：根据date是否为空动态计算startTime ==========
//	    Date startTime = null;
//	    // 1. 判断date是否为空/Null
//	    if (date == null || date.trim().isEmpty()) {
//	        // date为空 → 使用timeRangeType计算起始时间
//	        startTime = calculateStartTime(timeRangeType);
//	    } else {
//	        // date非空 → 将date转为Date类型作为startTime
//	        try {
//	            // 日期格式：yyyy-MM-dd（可根据实际业务调整）
//	            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//	            startTime = sdf.parse(date.trim());
//	        } catch (ParseException e) {
//	            // 日期格式错误时，降级使用timeRangeType（避免方法报错）
//	            System.err.println("日期格式错误（需为yyyy-MM-dd），降级使用时间范围：" + e.getMessage());
//	            startTime = calculateStartTime(timeRangeType);
//	        }
//	    }
//
//	    // 添加时间过滤条件（createTime >= startTime）
//	    query.addCriteria(Criteria.where("createTime").gte(startTime));
//
//	    return this.find(query, StockStatistics.class);
//	}
//	public List<StockStatistics> findStockStatisByStockId(String id, TimeRangeType timeRangeType,String date) {
//		Query query = new Query();
//		// 原有过滤条件（保持不变）
//		query.addCriteria(Criteria.where("revoke").is(false));
//		query.addCriteria(Criteria.where("isDelete").is(false));
//		query.addCriteria(Criteria.where("isDisable").is(false));
//		query.addCriteria(Criteria.where("inOrOut").is(true));
//		query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(id)));
//
//		// 根据时间范围类型，计算对应的起始时间
//		Date startTime = calculateStartTime(timeRangeType);
//		// 添加时间过滤条件（替换为你实际的时间字段名，比如statisTime/updateTime）
//		query.addCriteria(Criteria.where("createTime").gte(startTime));
//
//		return this.find(query, StockStatistics.class);
//	}
	

	/**
	 * 封装时间计算逻辑，按类型返回起始时间（当年/当月1日 00:00:00）
	 * 
	 * @param timeRangeType 时间范围类型
	 * @return 起始时间
	 */
	private Date calculateStartTime(TimeRangeType timeRangeType) {
		// 指定北京时间，避免时区偏差
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
		// 重置时分秒毫秒为0，确保起始时间是当天零点
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		switch (timeRangeType) {
		case CURRENT_YEAR:
			// 当年1月1日
			calendar.set(Calendar.MONTH, Calendar.JANUARY);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			break;
		case CURRENT_MONTH:
			// 当月1日
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			break;
		default:
			// 默认返回当年1月1日（避免空值）
			calendar.set(Calendar.MONTH, Calendar.JANUARY);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			break;
		}
		return calendar.getTime();
	}

	// 兼容原有方法（无参数时默认查当年数据）
//    public List<StockStatistics> findStockStatisByStockId(String id ) {
//        return findStockStatisByStockId(id, TimeRangeType.CURRENT_YEAR);
//    }
//	

	// 修复后的平均单价计算方法
	public BigDecimal calculateAveragePriceByStockId(String stockId, TimeRangeType t,String date) {
		List<StockStatistics> stockStatsList = findStockStatisByStockId(stockId, t,date);

		BigDecimal totalPriceNum = BigDecimal.ZERO; // price*num 的总和
		int totalNum = 0; // num 的总和

		for (StockStatistics stats : stockStatsList) {
			// 1. 先校验核心字段非空且数量有效
			if (stats == null || stats.getPrice() == null || stats.getNum() == null || stats.getNum() <= 0) {
				continue;
			}

			try {
				// 2. 统一转换为BigDecimal，兼容Double/Integer/BigDecimal类型
				BigDecimal priceBigDecimal = convertToBigDecimal(stats.getPrice());
				BigDecimal numBigDecimal = BigDecimal.valueOf(stats.getNum());

				// 3. 安全计算 price*num 并累加
				BigDecimal currentPriceNum = priceBigDecimal.multiply(numBigDecimal);
				totalPriceNum = totalPriceNum.add(currentPriceNum);
				totalNum += stats.getNum();

			} catch (Exception e) {
				// 捕获类型转换/计算异常，跳过这条无效数据
				System.err.println("处理数据异常，跳过：" + e.getMessage());
				continue;
			}
		}

		// 防止除数为0
		if (totalNum == 0) {
			return BigDecimal.ZERO;
		}

		// 保留2位小数，四舍五入
//		return totalPriceNum.divide(BigDecimal.valueOf(totalNum));
		return totalPriceNum.divide(BigDecimal.valueOf(totalNum), 2, RoundingMode.HALF_UP);
	}
	
	/**
	 * 获取指定设备当月的入库数量（复用原有查询逻辑）
	 * @param stockId 设备ID
	 * @param t 时间范围类型
	 * @return 当月入库总数量（int类型，无数据返回0）
	 */
	public BigDecimal getCurrentMonthInStockNum(String stockId, TimeRangeType t,String date) {
	    // 复用原有查询逻辑，避免重复查库
	    List<StockStatistics> stockStatsList = findStockStatisByStockId(stockId, t,date);
	    
	    // 核心修改：改为BigDecimal类型，支持小数累加
	    BigDecimal inStockTotalNum = BigDecimal.ZERO; // 当月入库总数量（支持小数）
	    
	    for (StockStatistics stats : stockStatsList) {
	        // 校验数量字段有效性（保留原有规则，仅调整类型判断）
	        if (stats == null || stats.getNum() == null || stats.getNum() <= 0) {
	            continue;
	        }
	        
	        try {
	            // 将数量转换为BigDecimal（兼容小数）
	            BigDecimal numBigDecimal = BigDecimal.valueOf(stats.getNum());
	            // 累加入库数量（使用BigDecimal.add保证精度）
	            inStockTotalNum = inStockTotalNum.add(numBigDecimal);
	        } catch (Exception e) {
	            System.err.println("累加入库数量异常，设备ID：" + stockId + "，异常信息：" + e.getMessage());
	            continue;
	        }
	    }
	    
	    // 统一保留2位小数（根据业务需求调整，如不需要可移除）
	    return inStockTotalNum.setScale(2, RoundingMode.HALF_UP);
	}
	
	
	
	
//	 changeDj = this.calculateAveragePriceByStockId(gs.getStock().getId(), TimeRangeType.CURRENT_MONTH);
//	public BigDecimal calculateAveragePriceByStockIdAndQichu(String stockId, TimeRangeType t) {
//	    // ========== 1. 重构日期获取逻辑：Date转String适配工具类 ==========
//	    Date startTime = calculateStartTime(t);
//	    String startTimeStr = convertDateToString(startTime, "yyyy-MM-dd");
//	    String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(startTimeStr);
//	    List<MonthEndStatistics> findMonthEndStatisticsByDate = this.monthEndStatisticsService
//	            .findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
//
//	    // ========== 2. 查询设备的库存统计数据（计算本月核心数据） ==========
//	    List<StockStatistics> stockStatsList = findStockStatisByStockId(stockId, t);
//
//	    BigDecimal thisMonthTotalPriceNum = BigDecimal.ZERO; // 本月：price*num 的总和（本月总价）
//	    int thisMonthTotalNum = 0;                          // 本月：数量总和
//	    BigDecimal thisMonthAvgPrice = BigDecimal.ZERO;     // 本月：平均单价（本月总价/本月总数量）
//
//	    for (StockStatistics stats : stockStatsList) {
//	        if (stats == null || stats.getPrice() == null || stats.getNum() == null || stats.getNum() <= 0) {
//	            continue;
//	        }
//
//	        try {
//	            BigDecimal priceBigDecimal = convertToBigDecimal(stats.getPrice());
//	            BigDecimal numBigDecimal = BigDecimal.valueOf(stats.getNum());
//	            // 累加本月总价（price*num）
//	            thisMonthTotalPriceNum = thisMonthTotalPriceNum.add(priceBigDecimal.multiply(numBigDecimal));
//	            // 累加本月总数量
//	            thisMonthTotalNum += stats.getNum();
//
//	        } catch (Exception e) {
//	            System.err.println("处理库存统计数据异常，设备ID：" + stockId + "，异常信息：" + e.getMessage());
//	            continue;
//	        }
//	    }
//
//	    // 计算本月平均单价（避免除零）
//	    if (thisMonthTotalNum > 0) {
//	        thisMonthAvgPrice = thisMonthTotalPriceNum.divide(
//	            BigDecimal.valueOf(thisMonthTotalNum), 2, RoundingMode.HALF_UP
//	        );
//	    }
//
//	    // ========== 3. 提取上月月度统计数据 ==========
//	    Optional<MonthEndStatistics> monthEndStatOpt = findMonthEndStatisticsByDate.stream()
//	            .filter(stat -> Objects.nonNull(stat.getStock()) 
//	                    && stockId.equals(stat.getStock().getId()))
//	            .findFirst();
//
//	    // 上月核心数据
//	    BigDecimal lastMonthPrice = BigDecimal.ZERO;    // 上月单价
//	    BigDecimal lastMonthNum = BigDecimal.ZERO;     // 上月数量
//	    BigDecimal lastMonthTotalPrice = BigDecimal.ZERO;// 上月总价（上月单价×上月数量）
//
//	    if (monthEndStatOpt.isPresent()) {
//	        MonthEndStatistics monthEndStat = monthEndStatOpt.get();
//	        // 校验并赋值上月单价
//	        if (Common.isNotEmpty(monthEndStat.getPrice()) && Common.isNumeric(monthEndStat.getPrice())) {
//	            lastMonthPrice = new BigDecimal(monthEndStat.getPrice());
//	        }
//	        // 校验并赋值上月数量
//	        if (monthEndStat.getMonthEndStockNum() != null && monthEndStat.getMonthEndStockNum() > 0) {
//	            lastMonthNum = BigDecimal.valueOf(monthEndStat.getMonthEndStockNum());
//	        }
//	        // 计算上月总价
//	        lastMonthTotalPrice = lastMonthPrice.multiply(lastMonthNum).setScale(2, RoundingMode.HALF_UP);
//	    }
//
//	    // ========== 4. 最终单价计算（核心修正） ==========
//	    // 本月总价 = 本月平均单价 × 本月总数量
//	    BigDecimal thisMonthTotalPrice = thisMonthAvgPrice.multiply(BigDecimal.valueOf(thisMonthTotalNum))
//	            .setScale(2, RoundingMode.HALF_UP);
//	    
//	    // 总数量 = 上月数量 + 本月数量
//	    BigDecimal totalCount = lastMonthNum.add(BigDecimal.valueOf(thisMonthTotalNum));
//	    // 总价格 = 上月总价 + 本月总价
//	    BigDecimal totalAllPrice = lastMonthTotalPrice.add(thisMonthTotalPrice);
//
//	    // 防止除数为0
//	    if (totalCount.compareTo(BigDecimal.ZERO) == 0) {
//	        return BigDecimal.ZERO;
//	    }
//
//	    // 最终平均单价 = (上月总价 + 本月总价) / (上月数量 + 本月数量)
//	    return totalAllPrice.divide(totalCount, 2, RoundingMode.HALF_UP);
//	}

	// ========== 新增：Date转String工具方法（可放入Common类，或作为当前类私有方法） ==========
	/**
	 * Date类型转换为指定格式的字符串
	 * @param date 待转换的Date对象（可为null）
	 * @param format 日期格式（如：yyyy-MM-dd）
	 * @return 格式化后的字符串，null则返回空字符串
	 */
	private String convertDateToString(Date date, String format) {
	    if (date == null) {
	        return ""; // 空值兜底，避免工具类解析报错
	    }
	    try {
	        SimpleDateFormat sdf = new SimpleDateFormat(format);
	        return sdf.format(date);
	    } catch (Exception e) {
	        System.err.println("日期格式化失败：" + e.getMessage());
	        return ""; // 异常兜底
	    }
	}


	// 新增：通用类型转换方法，将任意数值类型转为BigDecimal
	private BigDecimal convertToBigDecimal(Object value) {
		if (value == null) {
			return BigDecimal.ZERO;
		}
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		} else if (value instanceof Double) {
			return BigDecimal.valueOf((Double) value);
		} else if (value instanceof Integer) {
			return BigDecimal.valueOf((Integer) value);
		} else if (value instanceof Float) {
			return BigDecimal.valueOf((Float) value);
		} else if (value instanceof Long) {
			return BigDecimal.valueOf((Long) value);
		} else {
			// 尝试将字符串转为数值（如果price是字符串类型）
			try {
				return new BigDecimal(value.toString());
			} catch (Exception e) {
				throw new IllegalArgumentException("不支持的价格类型：" + value.getClass().getName());
			}
		}
	}

	/**
	 * 计算期初+当月的综合平均单价
	 * 公式：((当月平均单价×当月入库数量) + (上月单价×上月库存数量)) / (当月入库数量 + 上月库存数量)
	 * @param stockId 设备ID
	 * @param t 时间范围类型（推荐传入TimeRangeType.CURRENT_MONTH）
	 * @return 最终综合单价（保留2位小数，四舍五入）
	 */
	public BigDecimal getQC(String stockId, TimeRangeType t, String date) {
	    // ========== 1. 获取当月核心数据 ==========
	    // 1.1 获取当月平均单价
	    BigDecimal currentMonthAvgPrice = calculateAveragePriceByStockId(stockId, t,date);
	    // 1.2 获取当月入库数量（支持小数）
	    BigDecimal currentMonthInStockNum = getCurrentMonthInStockNum(stockId, t,date);
	   
	    
	    // ========== 2. 获取上月月度统计数据 ==========
	    // 2.1 从TimeRangeType中解析日期（假设你有requestBo对象，或从t中获取日期）
	    // 注：若requestBo未在当前方法作用域，需补充日期获取逻辑（如从t中提取）
	    String startDateStr = convertDateToString (calculateStartTime(t),"yyyy-MM-dd");
	    // 2.2 获取上月最后一天
	    String lastDayOfPreviousMonthAsString = Common.getLastDayOfPreviousMonthAsString(date==""?startDateStr:date);
	    // 2.3 查询上月最后一天的月度统计数据
	    List<MonthEndStatistics> lastMonthStatsList = this.monthEndStatisticsService
	            .findMonthEndStatisticsByDate(lastDayOfPreviousMonthAsString);
	    // 2.4 转换为设备ID映射的Map
	    Map<String, MonthEndStatistics> lastMonthStatsMap = listMonthEndStatisticsToMap(lastMonthStatsList);
	    
	    // ========== 3. 提取当前设备的上月统计数据 ==========
	    MonthEndStatistics lastMonthStats = lastMonthStatsMap.get(stockId);
	    // 初始化上月数据（默认0）
	    BigDecimal lastMonthPrice = BigDecimal.ZERO;    // 上月平均单价
	    BigDecimal lastMonthStockNum = BigDecimal.ZERO; // 上月月末库存数量
	    
	    if (Objects.nonNull(lastMonthStats)) {
	        // 3.1 校验并转换上月单价（String转BigDecimal）
	        if (Common.isNotEmpty(lastMonthStats.getPrice()) && Common.isNumeric(lastMonthStats.getPrice())) {
	            lastMonthPrice = new BigDecimal(lastMonthStats.getPrice());
	        }
	        // 3.2 校验并转换上月库存数量（Double转BigDecimal）
	        if (Objects.nonNull(lastMonthStats.getMonthEndStockNum()) && lastMonthStats.getMonthEndStockNum() > 0) {
	            lastMonthStockNum = BigDecimal.valueOf(lastMonthStats.getMonthEndStockNum());
	        }
	    }
	    
	    // ========== 4. 核心公式计算 ==========
	    // 4.1 计算当月总价：当月平均单价 × 当月入库数量
	    BigDecimal currentMonthTotalPrice = currentMonthAvgPrice.multiply(currentMonthInStockNum)
	            .setScale(2, RoundingMode.HALF_UP);
	    // 4.2 计算上月总价：上月单价 × 上月库存数量
	    BigDecimal lastMonthTotalPrice = lastMonthPrice.multiply(lastMonthStockNum)
	            .setScale(2, RoundingMode.HALF_UP);
	    // 4.3 计算总价格：当月总价 + 上月总价
	    BigDecimal totalPrice = currentMonthTotalPrice.add(lastMonthTotalPrice);
	    // 4.4 计算总数量：当月入库数量 + 上月库存数量
	    BigDecimal totalNum = currentMonthInStockNum.add(lastMonthStockNum);
	    
	    // ========== 5. 防除零处理 & 最终单价计算 ==========
	    if (totalNum.compareTo(BigDecimal.ZERO) == 0) {
	        return BigDecimal.ZERO; // 无数据时返回0
	    }
	    // 最终单价 = 总价格 / 总数量（保留2位小数，四舍五入）
	    return totalPrice.divide(totalNum, 2, RoundingMode.HALF_UP);
	}


}
