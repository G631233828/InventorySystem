package zhongchiedu.inventory.service.Impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpSession;

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
			String end, String type) {

		// 分页查询数据
		Pagination<StockStatistics> pagination = null;
		try {
			Query query = new Query();
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
			ca1.orOperator(Criteria.where("stock.$id").in(stockId),Criteria.where("user.$id").in(userId), Criteria.where("name").regex(search));
		}
		
		if(Common.isNotEmpty(start)&&Common.isNotEmpty(end)){
			ca2.orOperator(Criteria.where("storageTime").gte(start).lte(end),Criteria.where("depotTime").gte(start).lte(end));
		}
		query.addCriteria(ca.andOperator(ca1,ca2));
		
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

}
