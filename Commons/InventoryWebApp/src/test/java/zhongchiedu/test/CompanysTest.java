package zhongchiedu.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.service.Impl.CompanyServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class CompanysTest {
	
	@Autowired
	private CompanyServiceImpl companyServiceImpl;
	
	@Test
	public void test1(){
		Companys c = new Companys();
		c.setName("北京银河技术有限公司");
		c.setAddress("北京");
		c.setContact("222222");
		this.companyServiceImpl.saveOrUpdate(c);
		
	}
	
	@Test
	public void testdelete(){

		String id = "5cd53b10a60d65d40c8fb65f";
		String re = this.companyServiceImpl.delete(id);
		System.out.println(re);
		
		
	}
	
	
	

}
