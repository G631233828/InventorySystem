package zhongchiedu.inventory.service.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.MonthEndStatistics;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.service.MonthEndStatisticsService;
import zhongchiedu.inventory.service.StockService;
@Service
@Slf4j
public class MonthEndStatisticsServiceImpl extends GeneralServiceImpl<MonthEndStatistics> implements MonthEndStatisticsService {
	
	@Autowired
	private StockService stockService;

	@Override
	public void automaticStatistics(String date) {
		//拿到所有库存的数据
		List<Stock> findAllStock = this.stockService.findAllStock();
		findAllStock.forEach(stock->{
			log.info("记录库存设备{},数量{}",stock.getName(),stock.getInventory());
			MonthEndStatistics  ms = new MonthEndStatistics();
			ms.setMonthEndStockNum(stock.getInventory());//库存数量
			ms.setDate(Common.isEmpty(date)?Common.fromDateYMD():date);
			ms.setStockName(stock.getName());
			ms.setStockModel(stock.getModel());
			ms.setStockSuppier(stock.getSupplier().getName());
			ms.setStockArea(stock.getArea().getName());
			ms.setStock(stock);
			this.save(ms);
		});
		
	}

	@Override
	public List<MonthEndStatistics> findMonthEndStatisticsByDate(String date) {
		Query query = new Query();
		query.addCriteria(Criteria.where("date").is(date));
		return this.find(query, MonthEndStatistics.class);
	}

	
	
}
