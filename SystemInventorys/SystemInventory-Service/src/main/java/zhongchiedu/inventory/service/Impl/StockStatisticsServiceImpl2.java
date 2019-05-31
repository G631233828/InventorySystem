//package zhongchiedu.inventory.service.Impl;
//
//import java.util.Date;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
//import javax.servlet.http.HttpSession;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.stereotype.Service;
//
//import lombok.extern.slf4j.Slf4j;
//import zhongchiedu.common.utils.BasicDataResult;
//import zhongchiedu.common.utils.Common;
//import zhongchiedu.common.utils.Contents;
//import zhongchiedu.framework.pagination.Pagination;
//import zhongchiedu.framework.service.GeneralServiceImpl;
//import zhongchiedu.general.pojo.User;
//import zhongchiedu.inventory.pojo.Stock;
//import zhongchiedu.inventory.pojo.StockStatistics;
//import zhongchiedu.inventory.service.StockStatisticsService;
//
//@Service
//@Slf4j
//public class StockStatisticsServiceImpl2 extends GeneralServiceImpl<StockStatistics> implements StockStatisticsService {
//
//	private @Autowired StockServiceImpl stockService;
//
//	@Override
//	public Pagination<StockStatistics> findpagination(Integer pageNo, Integer pageSize, boolean inOrOut) {
//
//		// 分页查询数据
//		Pagination<StockStatistics> pagination = null;
//		try {
//			Query query = new Query();
//			query.addCriteria(Criteria.where("inOrOut").is(inOrOut));
//			pagination = this.findPaginationByQuery(query, pageNo, pageSize, StockStatistics.class);
//			if (pagination == null)
//				pagination = new Pagination<StockStatistics>();
//			return pagination;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return pagination;
//	}
//
//	@Override
//	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics, HttpSession session) {
//
//		long num = stockStatistics.getNum();
//		if(num<=0){
//			return BasicDataResult.build(400, "操作的数据有误！", null);
//		}
//		String id = stockStatistics.getStock().getId();// 获取库存设备id
//		Stock stock = this.stockService.findOneById(id, Stock.class);
//		if (Common.isNotEmpty(stock)) {
//			User user =null;// (User) session.getAttribute(Contents.USER_SESSION);
//			stockStatistics.setStorageTime(new Date());
//			stockStatistics.setUser(user);
//			stockStatistics.setRevoke(false);
//			synchronized (id) { // 对当前id进行锁定
//				if (stockStatistics.isInOrOut()) {
//					// true == 入库
//						// 更新库存中的库存
//						long newNum = this.updateStock(stock,num , true);
//						stockStatistics.setNewNum(newNum);
//						lockInsert(stockStatistics);
//						return BasicDataResult.build(200, "商品入库成功", null);
//				} else {
//					// 出库
//					long newNum = this.updateStock(stock,num, false);
//					if(newNum == -1){
//						//出货数量不够
//						return BasicDataResult.build(400, "货物库存数量不足", null);
//					}
//					stockStatistics.setNewNum(newNum);;
//					lockInsert(stockStatistics);
//					return BasicDataResult.build(200, "商品出库成功", null);
//				}
//			}
//		} else {
//			// 未能找到库存的信息，反馈界面入库失败
//			return BasicDataResult.build(400, "未能找到库存商品", null);
//		}
//	}
//
//	Lock lock = new ReentrantLock();
//	Lock lockinsert = new ReentrantLock();
//	
//	
//	public void lockInsert(StockStatistics stockStatistics){
//		
//		lockinsert.lock();
//		try{
//			this.insert(stockStatistics);
//		}finally{
//			lockinsert.unlock();
//		}
//		
//		
//	}
//	
//	
//	
//	
//	
//
//	public long updateStock(Stock stock, long num, boolean inOrOut) {
//		lock.lock();
//		long oldnum = stock.getInventory();
//		long newnum = 0;
//		try {
//			if(inOrOut){
//				//入库
//				newnum = oldnum + num;
//				stock.setInventory(newnum);
//				this.stockService.save(stock);
//				return newnum;
//			}else{
//				//出库
//				if((oldnum - num)<0){
//					return -1;
//				}
//				newnum = oldnum - num;
//				stock.setInventory(newnum);
//				this.stockService.save(stock);
//				return newnum;
//			}
//		} finally {
//			lock.unlock();
//		}
//
//	}
//
//}
