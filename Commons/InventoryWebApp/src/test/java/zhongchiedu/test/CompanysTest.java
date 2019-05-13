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
	public void test1() {
		Companys c = new Companys();
		c.setName("北京银河技术有限公司");
		c.setAddress("北京");
		c.setContact("222222");
		this.companyServiceImpl.saveOrUpdate(c);

	}

	
	@Test
	public void testThread(){
		
		for (int i = 0; i < 3; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					testdelete(Thread.currentThread().getName());
					
				}
			}).start();
		}
	
		
	}
	
	
	
	public void testdelete(String name) {
		System.out.println(name);
		String id = "5cd53b10a60d65d40c8fb65f,5cd53b3aa60d65dd38ca2b3a,5cd53cd9a60d65df58fe5eb8";
		for (int i = 0; i < 5; i++) {
			String re = companyServiceImpl.delete(id);
			System.out.println(re);
		}
		
		

	}

}
