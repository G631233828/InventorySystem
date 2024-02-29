package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
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
import zhongchiedu.common.utils.*;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.MultiMediaService;
import zhongchiedu.general.service.Impl.UserServiceImpl;
import zhongchiedu.inventory.pojo.*;
import zhongchiedu.inventory.service.*;
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
	
	private @Autowired PickUpApplicationService pickUpApplicationService;

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
			String end, String type, String id, String searchArea, String searchAgent,String userId,String revoke,String confirm,String ssC) {

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
			if(Common.isNotEmpty(revoke)) {
				if(revoke.equals("1")) {
					query.addCriteria(Criteria.where("revoke").is(true));
				}else if(revoke.equals("2")) {
					query.addCriteria(Criteria.where("revoke").is(false));
				}
			}
			query = this.findbySearch(search, start, end, type, query, searchArea,ssC);
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

	public Pagination<StockStatistics> findpagination(Integer pageNo, Integer pageSize, RequestBo requestBo){
		// 分页查询数据
		Pagination<StockStatistics> pagination = null;
		try {
			Query query=newQueryByRequestBo(requestBo);
			if(Common.isNotEmpty(requestBo.getRevoke())) {
				if(requestBo.getRevoke().equals("1")) {
					query.addCriteria(Criteria.where("revoke").is(true));
				}else if(requestBo.getRevoke().equals("2")) {
					query.addCriteria(Criteria.where("revoke").is(false));
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
	public Query findbySearch(String search, String start, String end, String type, Query query, String areaId,String ssC) {
		Criteria ca = new Criteria();
		Criteria ca1 = new Criteria();
		Criteria ca2 = new Criteria();
		Criteria ca3 = new Criteria();
		end=end+" 23:59:59";
		if (type.equals("in")) {
			query.addCriteria(Criteria.where("inOrOut").is(true));
		} else if (type.equals("out")) {
			query.addCriteria(Criteria.where("inOrOut").is(false));
		}

		if (Common.isNotEmpty(areaId)) {
			query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		List<Object> stockIdByssC=null;
		if(Common.isNotEmpty(ssC)){
			stockIdByssC=findStocksByssCId(ssC);
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
					Criteria.where("newItemNo").regex(search),
					Criteria.where("outboundOrder").regex(search),
					Criteria.where("accepter").regex(search),
					Criteria.where("paymentOrderNo").regex(search));
		}


		if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
			ca2.orOperator(Criteria.where("storageTime").gte(start).lte(end),
					Criteria.where("depotTime").gte(start).lte(end)
//					,
//					Criteria.where("editFinanceTime").gte(start).lte(end),
//					Criteria.where("sailesInvoiceDate").gte(start).lte(end)
					);
		}
		query.addCriteria(ca.andOperator(ca1, ca2,ca3));

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
		ca.orOperator(
				Criteria.where("entryName").regex(search), Criteria.where("itemNo").regex(search),
				Criteria.where("name").regex(search,"i"),
				Criteria.where("supplier.$id").in(findSupplierIds),
				Criteria.where("projectLeader").regex(search),
				Criteria.where("model").regex(search,"i"));
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
	public List<Object> findStocksByssCId(String ssC){
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query = query.addCriteria(Criteria.where("systemClassification.$id").is(new ObjectId(ssC)));
		List<Stock> lists=this.stockService.find(query,Stock.class);
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
			query.addCriteria(
					ca.orOperator(Criteria.where("name").regex(search), Criteria.where("model").regex(search)));

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
	
		long num =0;
		if(stockStatistics.isRevoke()) {
			num = stockStatistics.getRevokeNum();//获取到撤销数量
		}else {
			num = stockStatistics.getNum();
			if (num <= 0) {
				return BasicDataResult.build(400, "操作的数据有误！", null);
			}
		}
		
		//如果撤销数量为0 说明是撤销全部
		if(num == 0) {
			num = stockStatistics.getNum();
		}
		
		String id = stockStatistics.getStock().getId();// 获取库存设备id
		Stock stock = this.stockService.findOneById(id, Stock.class);
	
		
		if (stock != null) {

			long ycknum = 0;
			long acnum = 0;
			if(!stockStatistics.isYck()) {
				List<PickUpApplication> pickUpApplication = this.pickUpApplicationService.findPickUpApplicationsByStockId(stock.getId());
				 ycknum = pickUpApplication.stream().map(PickUpApplication::getEstimatedIssueQuantity).reduce((long) 0,Long::sum);
				 
				 acnum = pickUpApplication.stream().map(PickUpApplication::getActualIssueQuantity).reduce((long) 0,Long::sum);
				
				 ycknum = ycknum - acnum;
			}
			
			stock.setDescription(stockStatistics.getDescription());
			stockStatistics.setUser(user);
//			stockStatistics.setRevoke(false);
			stockStatistics.setArea(stock.getArea());
			stockStatistics.setAgent(stock.isAgent());
			if (stockStatistics.isInOrOut()) {
				// true == 入库
				// 更新库存中的库存
				long newNum = this.updateStock(stock, num, true);
				
				stockStatistics.setNewNum(newNum);
				if(stockStatistics.isRevoke()) {
					stockStatistics.setNum(stockStatistics.getNum()-num);
					stockStatistics.setRevokeNum(num);
					stockStatistics.setInOrOut(false);
					stockStatistics.setRevoke((stockStatistics.getNum()-num)<=0);
				}else {
					stockStatistics.setStorageTime(Common.fromDateH());
					stockStatistics.setNum(stockStatistics.getNum());
				}
				stockStatistics.setRemainingNum( newNum- ycknum);
				lockUpdate(stockStatistics);

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
				stockStatistics.setRemainingNum( newNum- ycknum);
				lockUpdate(stockStatistics);
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
	public void lockUpdate(StockStatistics stockStatistics) {

		lockinsert.lock();
		try {
		
			this.save(stockStatistics);
			
			
			
		} finally {
			lockinsert.unlock();
		}

	}

	@SystemServiceLog(description = "更新库存信息")
	public long updateStock(Stock stock, long num, boolean inOrOut) {
		lock.lock();
		
		
		long oldnum = stock.getInventory();
		
		
		long newnum = 0;
		try {
			if (inOrOut) {
				// 入库
				newnum = oldnum + num;
				stock.setInventory(newnum);
				stock.setIsDelete(false);
				this.stockService.save(stock);
				return newnum;
			} else {
				// 出库
				if ((oldnum - num) < 0) {
					return -1;
				}
				newnum = oldnum - num;
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
	public long updatePreStock(Stock stock, long num, boolean inOrOut,StockStatistics st) {
		lock.lock();
		long oldnum = stock.getInventory();
		long newnum = 0;
		try {
				// 撤销入库
				if ((oldnum - num) < 0) {
					return -1;
				}
				if(st.isPreStock()){
				PreStock preStock=preStockService.findOneById(st.getPreStockId(),PreStock.class);
				long renum=preStock.getActualReceiptQuantity()-num;
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

	}


	@Override
	@SystemServiceLog(description = "撤销库存信息")
	public BasicDataResult revoke(String id,long num,User user) {
		
		
		
		StockStatistics st = this.findOneById(id, StockStatistics.class);
		if (st.isRevoke()) {
			return BasicDataResult.build(400, "该信息中的设备已经全部撤销，不能重复撤销", null);

		}
		if (st.getStock() == null) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}
		
		if(num>st.getNum()) {
			return BasicDataResult.build(400, "撤销出库数量不能大于出库数量", null);
		}
		
		
		String stockId = st.getStock().getId();
		
		Stock stock = this.stockService.findOneById(stockId, Stock.class);
		if (stock == null) {
			return BasicDataResult.build(400, "未能获取到设备信息", null);
		}
		
		
		//根据设备id获取到预出库的信息
		List<PickUpApplication> pickUpApplication = this.pickUpApplicationService.findPickUpApplicationsByStockId(stockId);
		//获得预出库设备数量
		//long ycknum = pickUpApplication.stream().map(PickUpApplication::getEstimatedIssueQuantity).reduce((long) 0,Long::sum);
		
		

		long newNum = 0L;
		if (Common.isNotEmpty(st.getStorageTime())) {
			// 撤销入库
			newNum = this.updatePreStock(stock, st.getNum(), false,st);
		} else {
			// 撤销出库
			//如果num == 0 说明是返还全部 否则获取到num的数量

//			long stnum  =0;
//			if(num>0) {
//				stnum = num;
//			}else {
//				stnum = st.getNum();
//			}
//			
			
//			newNum = this.updateStock(stock, stnum, true );
			st.setInOrOut(true);
			st.setRevokeNum(num);
			st.setRevoke(true);
			//撤销出库通过统计去撤销并记录统计数据
			 BasicDataResult inOrOutstockStatistics = this.inOrOutstockStatistics(st, user);
			 if(inOrOutstockStatistics.getStatus()==200) {
				 return BasicDataResult.build(200, "撤销成功", inOrOutstockStatistics.getData());
			 }
			
		}
		
	
//		if (newNum == -1) {
//			// 出货数量不够
//			return BasicDataResult.build(400, "货物库存数量不足,无法撤销入库", null);
//		}
//		StockStatistics stockStatistics = updateStockStatistics(st);
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
	@SystemServiceLog(description = "导出库存统计信息")
	public Workbook newExport(HttpServletRequest request, RequestBo requestBo) {

//		List<Stock> listStock = this.findStocksBySearch(search, areaId, searchAgent);
		// 获取所有的库存
//		List<StockStatistics> list = this.findStockStatistics(search, start, end, type, areaId, searchAgent);

		Query querys = new Query();
		querys=this.stockService.findByRequestBo(requestBo,querys);
//		querys.addCriteria(Criteria.where("isDelete").is(false));
		List<Stock> listStock=this.stockService.find(querys,Stock.class);

		//获取所有的库存统计数据
		Query query=newQueryByRequestBo(requestBo);
		query.addCriteria(Criteria.where("revoke").is(false));
		List<StockStatistics> list=this.find(query,StockStatistics.class);

		List<Map<String, Object>> inlist = new ArrayList<>();
		List<Map<String, Object>> outlist = new ArrayList<>();

		for (Stock stock : listStock) {
			// 获取所有的设备
			for (StockStatistics st : list) {

				if (st.getStock() != null) {
					if (stock.getId().equals(st.getStock().getId())) {
						if (st.isInOrOut()) {
							// 入库统计
							Map<String, Object> in = new HashMap<>();
							in.put("itemNo", Common.isEmpty(stock.getItemNo()) ? "" : stock.getItemNo());
							in.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
							in.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
							in.put("stockName", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
							in.put("modelName",
									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
							in.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
							in.put("inprice", Common.isEmpty(st.getInprice()) ? "" : st.getInprice());
							in.put("unit",
									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
							in.put("depotTime", st.getStorageTime());
							in.put("num", st.getNum());
							in.put("purchaseInvoiceNo", Common.isEmpty(st.getPurchaseInvoiceNo()) ? ""
									: st.getPurchaseInvoiceNo());

							in.put("newItemNo",Common.isEmpty(st.getNewItemNo()) ? "" : st.getNewItemNo());
							in.put("paymentOrderNo", Common.isEmpty(st.getPaymentOrderNo()) ? ""
									: st.getPaymentOrderNo());
							in.put("supplier", Common.isEmpty(st.getStock().getSupplier()) ? ""
									: st.getStock().getSupplier().getName());
							in.put("purchaseInvoiceDate", Common.isEmpty(st.getPurchaseInvoiceDate()) ? ""
									: st.getPurchaseInvoiceDate());
							inlist.add(in);
						} else {
							Map<String, Object> out = new HashMap<>();
							out.put("itemNo", Common.isEmpty(stock.getItemNo()) ? "" : stock.getItemNo());
							out.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
							out.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
							out.put("stockName",
									Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
							out.put("modelName",
									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
							out.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
							out.put("unit",
									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
							out.put("depotTime", st.getDepotTime());
							out.put("num", st.getNum());
							out.put("sailesInvoiceNo",
									Common.isEmpty(st.getSailesInvoiceNo()) ? "" : st.getSailesInvoiceNo());
							out.put("sailPrice",
									Common.isEmpty(st.getSailPrice()) ? "" : st.getSailPrice());
							out.put("sailesInvoiceDate",
									Common.isEmpty(st.getSailesInvoiceDate()) ? "" : st.getSailesInvoiceDate());
							out.put("receiptNo",
									Common.isEmpty(st.getReceiptNo()) ? "" : st.getReceiptNo());
							out.put("customer", Common.isEmpty(st.getCustomer()) ? "" : st.getCustomer());
							out.put("purchaseInvoiceDate", Common.isEmpty(st.getPurchaseInvoiceDate()) ? ""
									: st.getPurchaseInvoiceDate());
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
		String fileName = "库存统计导出模板.xlsx";
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

	@Override
	@SystemServiceLog(description = "导出适配金蝶的报表")
	public Workbook toJD(HttpServletRequest request,RequestBo requestBo) {
		Query query=new Query();
		Criteria ca = new Criteria();
		List<Stock> listStock=null;
		if(!requestBo.isEmpty()){
			Query querys = new Query();
			querys=this.stockService.findByRequestBo(requestBo,querys);
			listStock=this.stockService.find(querys,Stock.class);
			List<Object> stockids=listStock.stream().map(stock -> new ObjectId(stock.getId())).collect(Collectors.toList());
			query=query.addCriteria(Criteria.where("stock.$id").in(stockids));
		}
		if (Common.isNotEmpty(requestBo.getId())) {
			ca.orOperator(Criteria.where("stock.$id").is(new ObjectId(requestBo.getId())));
		}
		if (Common.isNotEmpty(requestBo.getUserId())) {
			ca.orOperator(Criteria.where("financeUser.$id").is(new ObjectId(requestBo.getUserId())));
		}
		if(Common.isNotEmpty(requestBo.getItemNo())){
			query=query.addCriteria(Criteria.where("newItemNo").regex(requestBo.getItemNo(), "i"));
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
		if (Common.isNotEmpty(requestBo.getConfirm())) {
			query = query.addCriteria(Criteria.where("confirm").is(Boolean.valueOf(requestBo.getConfirm())));
		}
		if(Common.isNotEmpty(requestBo.getType())){
			if (requestBo.getType().equals("in")) {
				query.addCriteria(Criteria.where("inOrOut").is(true));
			} else if (requestBo.getType().equals("out")) {
				query.addCriteria(Criteria.where("inOrOut").is(false));
			}
		}
		if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
			String end=requestBo.getEnd();
			end=end+" 23:59:59";
			Criteria ca1 = new Criteria();
			ca1.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
					Criteria.where("depotTime").gte(requestBo.getStart()).lte(end)
			);
			ca.andOperator(ca1);
		}
		query.addCriteria(ca);
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		String end=requestBo.getEnd();
		query.addCriteria(Criteria.where("revoke").is(false));
		List<StockStatistics> list =this.find(query,StockStatistics.class);
		List<Map<String, Object>> inlist = new ArrayList<>();
		List<Map<String, Object>> outlist = new ArrayList<>();
		int inN=0;
		int OutN=0;
		for (Stock stock : listStock) {
			// 获取所有的设备
			for (StockStatistics st : list) {

				if (st.getStock() != null) {
					if (stock.getId().equals(st.getStock().getId())) {
						if (st.isInOrOut()) {
							// 入库统计
							Map<String, Object> in = new HashMap<>();
							//设备名称+型号+计量单位
							String str=(Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName())
									+(Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel())
									+(Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());

							in.put("itemNo", Common.isEmpty(stock.getItemNo()) ? "" : stock.getItemNo());
//							in.put("id", Common.isEmpty(st.getId()) ? "" : st.getId());
							in.put("id",createIdByStr(str));
							inN++;
							in.put("dj",createDJ(end,inN,""));
							in.put("cg","赊购");
							in.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
							in.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
							in.put("stockName", Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
							in.put("modelName",
									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
//							in.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
							//单价=入库总金额/入库数量
							in.put("price",devide(st.getInprice(),st.getNum()));
							in.put("unit",
									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
							in.put("depotTime", st.getStorageTime().substring(0,10));
							in.put("num", st.getNum());
							in.put("purchaseInvoiceNo", Common.isEmpty(st.getPurchaseInvoiceNo()) ? ""
									: st.getPurchaseInvoiceNo());
							in.put("supplier", Common.isEmpty(st.getStock().getSupplier()) ? ""
									: st.getStock().getSupplier().getName());

							in.put("purchaseInvoiceDate", Common.isEmpty(st.getPurchaseInvoiceDate()) ? ""
									: st.getPurchaseInvoiceDate());

							inlist.add(in);
						} else {
							Map<String, Object> out = new HashMap<>();
							//设备名称+型号+计量单位
							String str=(Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName())
									+(Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel())
									+(Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());

							out.put("itemNo", Common.isEmpty(stock.getItemNo()) ? "" : stock.getItemNo());
//							out.put("id", Common.isEmpty(st.getId()) ? "" : st.getId());
							out.put("id",createIdByStr(str));
							OutN++;
							out.put("dj",createDJ(end,OutN,""));
							out.put("fs","赊销");
							out.put("area", Common.isEmpty(stock.getArea()) ? "" : stock.getArea().getName());
							out.put("projectName", Common.isEmpty(st.getProjectName()) ? "" : st.getProjectName());
							out.put("stockName",
									Common.isEmpty(st.getStock().getName()) ? "" : st.getStock().getName());
							out.put("modelName",
									Common.isEmpty(st.getStock().getModel()) ? "" : st.getStock().getModel());
							out.put("price", Common.isEmpty(st.getStock().getPrice()) ? "" : st.getStock().getPrice());
							out.put("unit",
									Common.isEmpty(st.getStock().getUnit()) ? "" : st.getStock().getUnit().getName());
							out.put("depotTime", st.getDepotTime().substring(0,10));
							out.put("num", st.getNum());
							out.put("sailesInvoiceNo",
									Common.isEmpty(st.getSailesInvoiceNo()) ? "" : st.getSailesInvoiceNo());
							out.put("sailesInvoiceDate",
									Common.isEmpty(st.getSailesInvoiceDate()) ? "" : st.getSailesInvoiceDate());
							out.put("receiptNo",
									Common.isEmpty(st.getReceiptNo()) ? "" : st.getReceiptNo());
							out.put("customer", Common.isEmpty(st.getCustomer()) ? "" : st.getCustomer());
							out.put("purchaseInvoiceDate", Common.isEmpty(st.getPurchaseInvoiceDate()) ? ""
									: st.getPurchaseInvoiceDate());
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

   //生成固定的单据编号
	public String createDJ(String end,Integer i,String  tou){
		String endnew=end.replace("-","").substring(0,8);
		return tou+endnew + StringUtils.leftPad(String.valueOf(i), 8, '0');
	}


	public String createIdByStr(String str){
		String newStr=str.replace(".","");
		return PinyinTool.getPinYinHeadChar(newStr);
	}

	public BigDecimal devide(Double price,long num1){
		if(Common.isEmpty(price)){
			return new BigDecimal(0);
		}
		BigDecimal inprice=new BigDecimal(price);
		BigDecimal num=new BigDecimal(num1);
		return inprice.divide(num,2,BigDecimal.ROUND_HALF_UP);
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
		query = this.findbySearch(search, start, end, type, query, areaId,"");
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
		if (Common.isEmpty(stockStatistics.getOutboundOrder())) {
			System.out.println(stockStatistics.getStock() == null);
			// 如果订单号为空说明是1个设备（历史数据处理）
			if (stockStatistics.getStock() != null) {
				stock = new HashMap<>();
				stock.put("id", 1);
				stock.put("name", Common.isEmpty(stockStatistics.getStock().getName()) ? ""
						: stockStatistics.getStock().getName());
				stock.put("model", Common.isEmpty(stockStatistics.getStock().getModel()) ? ""
						: stockStatistics.getStock().getModel());
				stock.put("unitName",
						Common.isNotEmpty(stockStatistics.getStock().getUnit())
								? stockStatistics.getStock().getUnit().getName()
								: "");
				stock.put("num", stockStatistics.getNum());
				allnum = stockStatistics.getNum();
				stocks.add(stock);
			}
		} else {
			// 根据单号获取所有出库数据
			List<StockStatistics> stockStatisticsList = this.findByoutboundOrder(stockStatistics.getOutboundOrder());

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

		String outboundOrder = Common.isEmpty(stockStatistics.getOutboundOrder()) ? ""
				: stockStatistics.getOutboundOrder();
		dataMap.put("customer", Common.isEmpty(stockStatistics.getCustomer()) ? "" : stockStatistics.getCustomer());
		dataMap.put("personInCharge",
				Common.isEmpty(stockStatistics.getPersonInCharge()) ? "" : stockStatistics.getPersonInCharge());
		try {
			dataMap.put("createDate", Common.getDateYMDHM(stockStatistics.getDepotTime()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		dataMap.put("outboundOrder", outboundOrder);
		dataMap.put("description",
				Common.isEmpty(stockStatistics.getDescription()) ? "" : stockStatistics.getDescription());
		dataMap.put("username", Common.isEmpty(user.getUserName()) ? "" : user.getUserName());
		dataMap.put("projectName",
				Common.isEmpty(stockStatistics.getProjectName()) ? "" : stockStatistics.getProjectName());

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
		if (Common.isNotEmpty(stockStatisticsId)) {
			stock = this.findOneById(stockStatisticsId, StockStatistics.class);
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
				String projectname = Common.isNotEmpty(stock.getProjectName()) ? stock.getProjectName().trim() : "";
				String customer = Common.isNotEmpty(stock.getCustomer()) ? stock.getCustomer().trim() : "";
				if (stock.getOutboundOrder() != null) {
					File outputFile = new File(path + projectname + customer + stock.getOutboundOrder() + ".png");
					MatrixToImageWriter.writeToFile(bitMatrix, format, outputFile);
					// 保存图片信息
					MultiMedia saveQrCode = this.multiMediaService.saveQrCode(outputFile, dir, qrcodepath, "PHOTO");
					qrcode.setQrcode(saveQrCode);
					qrcode.setPath(urlpath);
					qrcode.setName(projectname + customer);
					qrcode.setType("STOCKSTATISTICS");
					this.qrCodeServce.insert(qrcode);
					stock.setQrCode(qrcode);
					this.save(stock);
				}

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

		List<StockStatistics> findByoutboundOrder = this.findByoutboundOrder(stockStatistics.getOutboundOrder());

		findByoutboundOrder.forEach(o -> {
			this.createStockStatisticsQrCode(o.getId());
		});

		return this.findOneById(id, StockStatistics.class);

	}

	@Override
	public Map<Object, Object> stockStatisticsPickup(StockStatistics stockStatistics,String i) {
		String outboundOrder = stockStatistics.getOutboundOrder();
		List<StockStatistics> st = this.findByoutboundOrder(outboundOrder);
		Map<Object, Object> map = new HashMap<>();
		map.put("personInCharge", st.get(0).getPersonInCharge());
		map.put("projectName", st.get(0).getProjectName());
		map.put("customer", st.get(0).getCustomer());
		map.put("description", st.get(0).getDescription());
		map.put("outboundOrder", st.get(0).getOutboundOrder());
		if(i.equals("sign")){
			//已经签名2个不用判断
		map.put("sign", st.get(0).getMysign().getSign());
		map.put("othersign",st.get(0).getOthersign().getSign());
		map.put("time", st.get(0).getPickupTime());
		}else{
			try {
				String dateYMDHM = Common.getDateYMDHM(new Date());
				stockStatistics.setPickupTime(dateYMDHM);

				map.put("time", dateYMDHM);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (Common.isEmpty(st.get(0).getMysign()) && Common.isEmpty(st.get(0).getOthersign())) {
				map.put("sign", stockStatistics.getSign());
				// 保存签名
				Sign sign = new Sign();
				sign.setSign(stockStatistics.getSign());
				this.signService.save(sign);

				st.forEach(s -> {
					s.setMysign(sign);
					s.setOpenId(stockStatistics.getOpenId());
					s.setPickupTime(stockStatistics.getPickupTime());
					this.save(s);
				});
			}else{
				//sign不为空，othersign为空的情况下，将form表单中的sign传入othersign，且保存pthersign
				map.put("sign", st.get(0).getMysign().getSign());
				map.put("othersign", stockStatistics.getSign());
				// 保存签名
				Sign sign = new Sign();
				sign.setSign(stockStatistics.getSign());
				this.signService.save(sign);
				st.forEach(s -> {
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
	public void updateStockStatistics(String ids, Double inprice, String purchaseInvoiceNo, String receiptNo,
			String paymentOrderNo, String sailesInvoiceNo,String sailesInvoiceDate,
									  User user,String purchaseInvoiceDate,Double sailPrice,String newItemNo,String description ) {

		List<String> array = Arrays.asList(ids.split(","));

		for (String id : array) {
			StockStatistics stockStatistics = this.findOneById(id, StockStatistics.class);
			if (inprice != null) {
				stockStatistics.setInprice(inprice);
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
			if(!newItemNo.equals("null")){
				stockStatistics.setNewItemNo(newItemNo);
			}
			if(!description.equals("null")){
				stockStatistics.setDescription(description);
			}
				stockStatistics.setEditFinanceTime(Common.fromDateH());
				stockStatistics.setFinanceUser(user);
			this.save(stockStatistics);
		}

	}

	@Override
	public Workbook newExport2(HttpServletRequest request, RequestBo requestBo) {
		Query query=newQueryByRequestBo(requestBo);
		query.addCriteria(Criteria.where("revoke").is(false));
		List<StockStatistics> list=this.find(query,StockStatistics.class);
		List<Map<String, Object>> outlist = new ArrayList<>();
		// 获取 start时间 （第一天）库存的初始数量 出库数量+剩余库存数量 num+newNum
		// 获取所有的设备
		Map<String, List<StockStatistics>> map = new HashMap<String, List<StockStatistics>>();
		
		//获取到所有库存的设备信息
		List<Stock> findAllStock = this.stockService.findAllStock();
		
		List<StockStatistics> list2 = findAllStock.stream().map(stock->{
			StockStatistics st = new StockStatistics();
			st.setNewNum(stock.getInventory());
			st.setNum(0);
			st.setStock(stock);
			return st;
		}).collect(Collectors.toList());
		
		list.addAll(list2);
		
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
		
		
		
		
		//集合的最后一条数据就是起初日期
		//遍历map ，将map数据放到outlist中
		
		
		for(Map.Entry<String, List<StockStatistics>> entry:map.entrySet()) {
			

			Map<String, Object> outmap = new HashMap<>();//定义输出到excel的map
			
			//期初库存 获取集合的最后一条数据为第一条记录
			
			StockStatistics gs = entry.getValue().get(entry.getValue().size()-1);
			//获取设备其他信息
			outmap.put("area", Common.isEmpty(gs.getArea()) ? "" : gs.getArea().getName());
			outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
			outmap.put("modelName",
					Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
			outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
			if(Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
				outmap.put("goodsStorage", Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())?"/"+gs.getStock().getGoodsStorage().getShelflevel():"");
			}
		
			outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent()?"是":"否");
			outmap.put("unit",
					Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());
			
			
			BigDecimal qcnum ;//期初库存 为当前时间的前一次库存数量  // new 包含本期未出入库，但有期末余额的库存，数量取期末余额。
			BigDecimal dj = new BigDecimal(0);//单价
			BigDecimal zj = new BigDecimal(0);//总金额
			BigDecimal newNum = new BigDecimal(gs.getNewNum());
			BigDecimal num =new BigDecimal( gs.getNum());
			if(gs.isInOrOut()) {
				//如果是入库，需要减去入库数量
				qcnum =  newNum.subtract(num);
			}else if(!gs.isInOrOut()){
				//如果是出库 需要吧出库数量加回去
				qcnum = newNum.add(num) ;//获得期初库存数量
			}else {
				qcnum = newNum;
			}
			if(Common.isNotEmpty(gs.getStock().getPrice())) {
			String price = gs.getStock().getPrice();
			boolean numeric = StringUtils.isNumeric(price);
			if(!numeric) {
				price ="0";
			}
			
				dj = new BigDecimal(price) ;//期初单价
				zj = qcnum.multiply(dj).setScale(2,BigDecimal.ROUND_HALF_UP);//出库总额
			}
		
			outmap.put("oldprice",dj);
			outmap.put("oldinventory",qcnum);
			outmap.put("oldpriceall",zj);
			
//			//20221123 添加出库入库总价
//			private Double inprice;

			long in =0;//入库数量
			//long inpriceall =0;//入库总价
			long out =0;//出库数量
//			int insize =0; //获入库次数
//			int outsize =0;//获取出库次数
			
			for (StockStatistics st : entry.getValue()) {
				
				if(st.isInOrOut()) {
					in+=st.getNum();//入库总数
//					insize++;//入库量计数
					//inpriceall+=Common.isNotEmpty(st.getInprice())?st.getInprice():0;//所有入库总额
				}else {
					out+=st.getNum();//出库总数
//					outsize++;//出库量计数
					
				}
			}
			
			
//			BigDecimal a = new BigDecimal(inpriceall);
			BigDecimal a = dj;
			BigDecimal b = new BigDecimal(in);
			
//			BigDecimal crkdj = new BigDecimal(0);
//			if(in>0) {
//				crkdj =  a.divide(b,2,BigDecimal.ROUND_HALF_UP);
//			}
			
			BigDecimal e = new BigDecimal(in);
			if(in >0) {
				BigDecimal	inpriceall =e.multiply(dj);
				outmap.put("inpriceall",inpriceall);//入库总额
			}else {
				outmap.put("inpriceall",0);//入库总额
			}
			
			
			outmap.put("in",in);//入库数量
			outmap.put("inprice",dj);//入库单价=所有入库总额/入库数量
			
			
			
			BigDecimal d = new BigDecimal(out);
//			BigDecimal e = new BigDecimal(in);
			BigDecimal outpriceall = d.multiply(dj).setScale(2,BigDecimal.ROUND_HALF_UP);//出库总额
			outmap.put("out",out);//出库数量
			outmap.put("outprice",dj);//出库单价=所有入库总额/入库数量
			outmap.put("outpriceall",outpriceall);//出库总额
			
			//期末库存数量  单价  金额
			StockStatistics gsend = entry.getValue().get(0);
			long qmnum =0;//期初库存
//			long qmdj =0;//单价
//			long qmzj =0;//总金额
			
			qmnum = gsend.getNewNum();//期末库存数量
			BigDecimal qmzj =dj.multiply(qcnum).setScale(2,BigDecimal.ROUND_HALF_UP);
//			BigDecimal qmzj =zj.add(a).subtract(outpriceall);//期末总价
//			BigDecimal qmdj = new BigDecimal(0);
//			if(qmnum>0) {
//				 qmdj =  qmzj.divide(new BigDecimal(qmnum),2,BigDecimal.ROUND_HALF_UP);
//			}
			outmap.put("newprice",dj);
			outmap.put("newinventory",qmnum);
			outmap.put("newpriceall",qmzj);
			outmap.put("purchaseInvoiceDate", Common.isEmpty(gs.getPurchaseInvoiceDate()) ? "": gs.getPurchaseInvoiceDate());
			outlist.add(outmap);
		
		}
		

		
		
		
		

		Map<String, Object> dataMap = new HashMap<>();

		dataMap.put("inlist", outlist);
		dataMap.put("title", requestBo.getStart() + "~" + requestBo.getEnd() + "库存统计");

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
	
	
	
	@Override
	public Workbook newExport3(HttpServletRequest request, String search, String start, String end, String type,
			String name, String areaId, String searchAgent) {

		// List<Stock> listStock = this.findStocksBySearch(search, areaId, searchAgent);
		//获取选择月份的统计数据
		List<StockStatistics> list1 = this.findStockStatistics(search, start, end, type, areaId, searchAgent);
		
		
		Map<String, String> findDateByInputDate = Common.findDateByInputDate(start);
		String ostart = findDateByInputDate.get("startDate");
		String oend = findDateByInputDate.get("endDate");
		
		List<StockStatistics> list2 = this.findStockStatistics(search, ostart, oend, type, areaId, searchAgent);
		   List<StockStatistics> list = list1.stream().filter(
			        item ->list2.stream().map(e -> e.getStock().getId())
			        .collect(Collectors.toList())
			        .contains(item.getStock().getId()))
			        .collect(Collectors.toList());
		
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
		//集合的最后一条数据就是起初日期
		//遍历map ，将map数据放到outlist中
		
		
		for(Map.Entry<String, List<StockStatistics>> entry:map.entrySet()) {
			

			Map<String, Object> outmap = new HashMap<>();//定义输出到excel的map
			
			//期初库存 获取集合的最后一条数据为第一条记录
			
			StockStatistics gs = entry.getValue().get(entry.getValue().size()-1);
			//获取设备其他信息
			outmap.put("area", Common.isEmpty(gs.getArea()) ? "" : gs.getArea().getName());
			outmap.put("stockName", Common.isEmpty(gs.getStock().getName()) ? "" : gs.getStock().getName());
			outmap.put("modelName",
					Common.isEmpty(gs.getStock().getModel()) ? "" : gs.getStock().getModel());
			outmap.put("scope", Common.isEmpty(gs.getStock().getScope()) ? "" : gs.getStock().getScope());
			if(Common.isNotEmpty(gs.getStock().getGoodsStorage())) {
				outmap.put("goodsStorage", Common.isNotEmpty(gs.getStock().getGoodsStorage().getShelflevel())?"/"+gs.getStock().getGoodsStorage().getShelflevel():"");
			}
		
			outmap.put("agent", Common.isEmpty(gs.getStock().isAgent()) ? "" : gs.getStock().isAgent()?"是":"否");
			outmap.put("unit",
					Common.isEmpty(gs.getStock().getUnit()) ? "" : gs.getStock().getUnit().getName());
			
			
			BigDecimal qcnum ;//期初库存 为当前时间的前一次库存数量
			BigDecimal dj = new BigDecimal(0);//单价
			BigDecimal zj = new BigDecimal(0);//总金额
			BigDecimal newNum = new BigDecimal(gs.getNewNum());
			BigDecimal num =new BigDecimal( gs.getNum());
			if(gs.isInOrOut()) {
				//如果是入库，需要减去入库数量
				qcnum =  newNum.subtract(num);
			}else {
				//如果是出库 需要吧出库数量加回去
				qcnum = newNum.add(num) ;//获得期初库存数量
			}
			if(Common.isNotEmpty(gs.getStock().getPrice())) {
			String price = gs.getStock().getPrice();
			boolean numeric = StringUtils.isNumeric(price);
			if(!numeric) {
				price ="0";
			}
			
				dj = new BigDecimal(price) ;//期初单价
				zj = qcnum.multiply(dj).setScale(2,BigDecimal.ROUND_HALF_UP);//出库总额
			}
		
			outmap.put("oldprice",dj);
			outmap.put("oldinventory",qcnum);
			outmap.put("oldpriceall",zj);
			
//			//20221123 添加出库入库总价
//			private Double inprice;

			long in =0;//入库数量
			long inpriceall =0;//入库总价
			long out =0;//出库数量
//			int insize =0; //获入库次数
//			int outsize =0;//获取出库次数
			
			for (StockStatistics st : entry.getValue()) {
				
				if(st.isInOrOut()) {
					in+=st.getNum();//入库总数
//					insize++;//入库量计数
					inpriceall+=Common.isNotEmpty(st.getInprice())?st.getInprice():0;//所有入库总额
				}else {
					out+=st.getNum();//出库总数
//					outsize++;//出库量计数
					
				}
			}
			
			
			BigDecimal a = new BigDecimal(inpriceall);
			BigDecimal b = new BigDecimal(in);
			
//			BigDecimal crkdj = new BigDecimal(0);
//			if(in>0) {
//				crkdj =  a.divide(b,2,BigDecimal.ROUND_HALF_UP);
//			}
			
			outmap.put("in",in);//入库数量
			outmap.put("inprice",dj);//入库单价=所有入库总额/入库数量
			outmap.put("inpriceall",inpriceall);//入库总额
			
			
			BigDecimal d = new BigDecimal(out);
//			BigDecimal e = new BigDecimal(in);
			BigDecimal outpriceall = d.multiply(dj).setScale(2,BigDecimal.ROUND_HALF_UP);//出库总额
			outmap.put("out",out);//出库数量
			outmap.put("outprice",dj);//出库单价=所有入库总额/入库数量
//			outmap.put("outprice",crkdj);//出库单价=所有入库总额/入库数量
			outmap.put("outpriceall",outpriceall);//出库总额
			
			//期末库存数量  单价  金额
			StockStatistics gsend = entry.getValue().get(0);
			long qmnum =0;//期初库存
//			long qmdj =0;//单价
//			long qmzj =0;//总金额
			
			qmnum = gsend.getNewNum();//期末库存数量
			BigDecimal qmzj =zj.add(a).subtract(outpriceall);//期末总价
			BigDecimal qmdj = new BigDecimal(0);
			if(qmnum>0) {
				 qmdj =  qmzj.divide(new BigDecimal(qmnum),2,BigDecimal.ROUND_HALF_UP);
			}
			outmap.put("newprice",qmdj);
			outmap.put("newinventory",qmnum);
			outmap.put("newpriceall",qmzj);
			outmap.put("purchaseInvoiceDate", Common.isEmpty(gs.getPurchaseInvoiceDate()) ? ""
					: gs.getPurchaseInvoiceDate());
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
	
	
	public Query  newQueryByRequestBo(RequestBo requestBo){
		Query query=new Query();
		Criteria ca = new Criteria();

		if(!requestBo.isEmpty()){
			Query querys = new Query();
			querys=this.stockService.findByRequestBo(requestBo,querys);
//			querys.addCriteria(Criteria.where("isDelete").is(false));
			List<Stock> stocks=this.stockService.find(querys,Stock.class);
			List<Object> stockids=stocks.stream().map(stock -> new ObjectId(stock.getId())).collect(Collectors.toList());
			query=query.addCriteria(Criteria.where("stock.$id").in(stockids));
		}
		if (Common.isNotEmpty(requestBo.getId())) {
			ca.orOperator(Criteria.where("stock.$id").is(new ObjectId(requestBo.getId())));
		}
		if (Common.isNotEmpty(requestBo.getUserId())) {
			ca.orOperator(Criteria.where("financeUser.$id").is(new ObjectId(requestBo.getUserId())));
		}
		if(Common.isNotEmpty(requestBo.getItemNo())){
			query=query.addCriteria(Criteria.where("newItemNo").regex(requestBo.getItemNo(), "i"));
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
		if(Common.isNotEmpty(requestBo.getProjectName())){
			query=query.addCriteria(Criteria.where("projectName").regex(requestBo.getProjectName(), "i"));
		}
		if(Common.isNotEmpty(requestBo.getCustomer())){
			query=query.addCriteria(Criteria.where("customer").regex(requestBo.getCustomer(), "i"));
		}
		if (Common.isNotEmpty(requestBo.getConfirm())) {
			query = query.addCriteria(Criteria.where("confirm").is(Boolean.valueOf(requestBo.getConfirm())));
		}
		if (Common.isNotEmpty(requestBo.getSailesInvoiceNo())) {
			query = query.addCriteria(Criteria.where("sailesInvoiceNo").regex(requestBo.getSailesInvoiceNo(), "i"));
		}
		if(Common.isNotEmpty(requestBo.getType())){
			if (requestBo.getType().equals("in")) {
				query.addCriteria(Criteria.where("inOrOut").is(true));
			} else if (requestBo.getType().equals("out")) {
				query.addCriteria(Criteria.where("inOrOut").is(false));
			}
		}
		if (Common.isNotEmpty(requestBo.getStart()) && Common.isNotEmpty(requestBo.getEnd())) {
			String end=requestBo.getEnd();
			end=end+" 23:59:59";
			Criteria ca1 = new Criteria();
			ca1.orOperator(Criteria.where("storageTime").gte(requestBo.getStart()).lte(end),
					Criteria.where("depotTime").gte(requestBo.getStart()).lte(end)
			);
			ca.andOperator(ca1);
		}
		query.addCriteria(ca);
		query.with(new Sort(new Order(Direction.DESC, "createTime")));
		return  query;
	}
	
	
	
	
	
	
	
	
	
	
	
	

	
	public static void main(String[] args) {
		
		
		long a =2004;
		double b =20.333f;
		BigDecimal bd = new BigDecimal(b);
		BigDecimal bd2 = new BigDecimal(a);
		BigDecimal divide = bd.divide(bd2,2,BigDecimal.ROUND_HALF_UP);
		BigDecimal multiply = bd.multiply(bd2).setScale(2,BigDecimal.ROUND_HALF_UP);
		System.out.println(multiply);
	}
	
	
}
