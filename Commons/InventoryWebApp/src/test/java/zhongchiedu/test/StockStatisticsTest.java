package zhongchiedu.test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.Impl.CompanyServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class StockStatisticsTest {

	private @Autowired StockStatisticsServiceImpl stockStatisticsService;
	private @Autowired StockServiceImpl stockService;
	private @Autowired CompanyServiceImpl companyService;

	//@Test
	public void test() {

		for (int i = 0; i < 10; i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {

					StockStatistics s = new StockStatistics();
					s.setInOrOut(true);// 入库
					s.setNum(1);
					stockStatisticsService.inOrOutstockStatistics(s, null);

				}
			}).start();
		}
		for (int i = 0; i < 5; i++) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					StockStatistics s = new StockStatistics();
					s.setInOrOut(false);// 入库
					s.setNum(1);
					stockStatisticsService.inOrOutstockStatistics(s, null);
					
				}
			}).start();
		}
	}

	private long nums = 1;

	

	public long getNums() {
		return nums;
	}

	public void setNums(long nums) {
		this.nums = nums;
	}

	public BasicDataResult inOrOutstockStatistics(StockStatistics stockStatistics, HttpSession session) {

		if (stockStatistics.isInOrOut()) {
			// true == 入库
			System.out.println("准备入库");
			long result = updateStock(stockStatistics.getNum(), true);
			System.out.println("商品入库成功" + result);
		} else {
			System.out.println("准备出库");
			long result = updateStock(stockStatistics.getNum(), false);
			System.out.println("商品出库成功" + result);
		}
		return null;
	}

	Lock lock = new ReentrantLock();
	Lock lockinsert = new ReentrantLock();

	public long updateStock(long num, boolean inOrOut) {
		lock.lock();
		long oldnum = this.getNums();
		long newnum = 0;
		try {
			if (inOrOut) {
				// 入库
				newnum = oldnum + num;
				this.setNums(newnum);
				return newnum;
			} else {
				// 出库
				if ((oldnum - num) < 0) {
					return -1;
				}
				newnum = oldnum - num;
				this.setNums(newnum);
				return newnum;
			}
		} finally {
			lock.unlock();
		}

	}
	
	
	
	

	
	
	
	
	
	
	
	
	
	
}
