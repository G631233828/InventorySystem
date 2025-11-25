package zhongchiedu.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import zhongchiedu.application.Application;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.service.StockService;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class TestInput {
	@Autowired
	private StockService stockService;

	
	
	
//	INSERT INTO [FUSHAN].[dbo].[StudentAccounts]
//	           ([SNO]
//	           ,[EntranceYear]
//	           ,[StudentName]
//	           ,[UserName]
//	           ,[Password]
//	           ,[SignID]
//	           ,[Locked]
//	           ,[ParentMobile]
//	           ,[SMS_Subscibed]
//	           ,[O365_Password])
//	     VALUES
//	           (
//				'20230101',
//	'2023',
//	'曹欣然',
//	'20230101',
//	'0MKYCC@2023',
//	0,
//	0,
//	0,
//	0,
//	NULL)

	
	
	@Test
	public void batchImport() {
		
		String insert = "INSERT INTO [FUSHAN].[dbo].[StudentAccounts]([SNO],[EntranceYear],[StudentName],[UserName],[Password],[SignID],[Locked],[ParentMobile],[SMS_Subscibed],[O365_Password])VALUES('v1','v2','v3','v4','v5',0,0,0,0,NULL);";
		File f = new File("d:/1.xls");

		String[][] resultexcel = null;
		try {
			resultexcel = ExcelReadUtil.readExcel(f, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int rowLength = resultexcel.length;
		ProcessInfo pri = new ProcessInfo();
		pri.allnum = rowLength;
		int j = 0;
		for (int i = 1; i < rowLength; i++) {
			String v1 = resultexcel[i][j].trim();// 账号
			String v2 = resultexcel[i][j+1].trim();//年份
			String v3 = resultexcel[i][j+2].trim();//姓名
			String v4 = resultexcel[i][j+3].trim();// 账号
			String v5 = resultexcel[i][j+4].trim();// 密码
			//System.out.println(v1+","+v2+","+v3+","+v4+","+v5);
		String replace = insert.replace("v1", v1).replace("v2", v2).replace("v3", v3).replace("v4", v4).replace("v5", v5);
			System.out.println(replace);
//			Stock stock = this.stockService.findByName(areaName, stockName, model, "");
//			System.out.println(stock);
			
			
		}
	}

}
















