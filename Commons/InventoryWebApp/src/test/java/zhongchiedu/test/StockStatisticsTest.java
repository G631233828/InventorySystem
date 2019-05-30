package zhongchiedu.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.Impl.CompanyServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class StockStatisticsTest {

	private  @Autowired StockStatisticsServiceImpl stockStatisticsService;
	private  @Autowired StockServiceImpl stockService;
	private  @Autowired CompanyServiceImpl companyService;

	@Test
	public void test() {
		StockStatisticsTest  t = new StockStatisticsTest();


			new Thread(new Runnable() {

				@Override
				public void run() {
					for (int i = 0; i < 10; i++) {
					Stock stock = stockService.findOneById("5cedddb7a60d651ab46bb8d5", Stock.class);
					Companys company = companyService.findOneById("5cd53b10a60d65d40c8fb65f", Companys.class);
					StockStatistics s = new StockStatistics();
					s.setStock(stock);
					s.setCompany(company);

					s.setInOrOut(true);// 入库
					s.setNum(1);
					t.execute(s);
					}
					
				}
			}).start();

	}

	public  void execute(StockStatistics s) {
		
		System.out.println(123);
		 this.stockStatisticsService.inOrOutstockStatistics(s, null);
	}

}
