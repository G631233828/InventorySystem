package zhongchiedu.test;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ColumnTest {

	@Autowired
	private ColumnServiceImpl columnService;

	@Test
	public void test1() {
//		private List<Category> categorys;  //目录列表
//		private Brand brand;//品牌
//		private String contact;//联系人
//		private String contactNumber;//联系电话
//		private String qq;
//		private String wechat;
//		private String email;
//		private String address;
//		private String introducer;//介绍人
//		private String productQuality;//产品质量
//		private String price;//价格
//		private String supplyPeriod;//供货期
//		private String payMent;//付款条件
//		private String afterSaleService;//售后服务
//		private List<String> showColumns;//显示列

		
		BasicDataResult result = this.columnService.editColumns("supplier", "name");
		
	}

	

}
