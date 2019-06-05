package zhongchiedu.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.service.Impl.CompanyServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class StockStatisticsfindBySearchTest {

	private @Autowired StockStatisticsServiceImpl stockStatisticsService;
	private @Autowired StockServiceImpl stockService;
	private @Autowired CompanyServiceImpl companyService;

//	@Test
//	public void test() {
//		
//		Pagination<StockStatistics> list = this.stockStatisticsService.findpagination(1, 100, "系统", "2019-05-04", "2019-06-01", "all");	
//		
//		System.out.println(list.getDatas().size());
//	}

	
	
}
	
