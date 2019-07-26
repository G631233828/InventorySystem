package zhongchiedu.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.service.Impl.ProjectStockServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ProjectStockTest {

	
	private @Autowired ProjectStockServiceImpl projectStockService;
	
	private @Autowired SupplierServiceImpl supplierService;
	

	@Test
	public void test() {
		Supplier supplier = this.supplierService.findOneById("5d11d848a60d651bd0e0990e1",Supplier.class);
		
		ProjectStock stock = this.projectStockService.findByName("123", "v3", "大网改造",supplier);
		
		System.out.println(stock);
		
		
	}

}
