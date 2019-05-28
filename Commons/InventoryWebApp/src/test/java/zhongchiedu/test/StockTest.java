package zhongchiedu.test;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class StockTest {
	
	private @Autowired StockServiceImpl stockService;
	
	private @Autowired GoodsStorageServiceImpl goodsStorageService;
	

	@Test
	public void test() {
		
		
		Pagination<Stock> p =this.stockService.findpagination(0, 10, "1111".trim());
		
		System.out.println(p.getDatas());
		
		
		
		
	}

}
