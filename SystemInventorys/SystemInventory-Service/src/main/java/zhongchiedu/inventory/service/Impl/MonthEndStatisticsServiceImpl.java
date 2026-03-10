package zhongchiedu.inventory.service.Impl;

import java.math.BigDecimal;
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
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl.TimeRangeType;
@Service
@Slf4j
public class MonthEndStatisticsServiceImpl extends GeneralServiceImpl<MonthEndStatistics> implements MonthEndStatisticsService {
	
	@Autowired
	private StockService stockService;
	
	@Autowired
	private StockStatisticsService stockStatisticsService;

//	@Override
//	public void automaticStatistics(String date) {
//		//拿到所有库存的数据
//		List<Stock> findAllStock = this.stockService.findAllStock();
//		findAllStock.forEach(stock->{
//			log.info("记录库存设备{},数量{}",stock.getName(),stock.getInventory());
//			MonthEndStatistics  ms = new MonthEndStatistics();
//			ms.setMonthEndStockNum(stock.getInventory());//库存数量
//			ms.setDate(Common.isEmpty(date)?Common.fromDateYMD():date);
//			ms.setStockName(stock.getName());
//			ms.setStockModel(stock.getModel());
//			ms.setStockSuppier(stock.getSupplier().getName());
//			ms.setStockArea(stock.getArea().getName());
//			ms.setStock(stock);
//			// 1. 接收计算结果
//			BigDecimal price = stockStatisticsService.calculateAveragePriceByStockId(stock.getId(), TimeRangeType.CURRENT_MONTH);
//			// 2. 空值处理 + 避免科学计数法
//			String priceStr = (price == null) ? stock.getPrice() : price.toPlainString();
//			// 3. 设置值
//			ms.setPrice(priceStr);
//			//月末更新最新单价
//			Stock getstock = this.stockService.findOneById(stock.getId(), Stock.class);
//			getstock.setPrice(priceStr);
//			this.stockService.save(getstock);
//			this.save(ms);
//		});
//		
//	}
	@Override
	public void automaticStatistics(String date) {
	    // 拿到所有库存的数据
	    List<Stock> findAllStock = this.stockService.findAllStock();
	    // 空集合防护
	    if (Common.isEmpty(findAllStock)) {
	        log.warn("自动统计库存：未查询到任何库存数据，跳过本次统计");
	        return;
	    }

	    findAllStock.forEach(stock -> {
	        try {
	            log.info("记录库存设备{},数量{}", stock.getName(), stock.getInventory());
	            MonthEndStatistics ms = new MonthEndStatistics();
	            ms.setMonthEndStockNum(stock.getInventory());//库存数量
	            ms.setDate(Common.isEmpty(date) ? Common.fromDateYMD() : date);
	            ms.setStockName(stock.getName());
	            ms.setStockModel(stock.getModel());
	            // 供应商/区域非空保护：避免 NPE
	            ms.setStockSuppier(stock.getSupplier() != null ? stock.getSupplier().getName() : "未知供应商");
	            ms.setStockArea(stock.getArea() != null ? stock.getArea().getName() : "未知区域");
	            ms.setStock(stock);

	            // 1. 计算平均价格（带异常捕获）
	            BigDecimal averagePrice = null;
	            try {
	                averagePrice = stockStatisticsService.calculateAveragePriceByStockId(stock.getId(), TimeRangeType.CURRENT_MONTH);
	            } catch (Exception e) {
	                log.error("计算库存[{}({})]平均价格失败", stock.getName(), stock.getId(), e);
	            }

	            // 2. 价格处理：多层非空/合法校验，避免空值/0值覆盖
	            String priceStr = null;
	            // 优先使用计算出的平均价格
	            if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) > 0) {
	                priceStr = averagePrice.toPlainString();
	            } 
	            // 平均价格无效时，使用库存原有价格（需校验）
	            else if (Common.isNotEmpty(stock.getPrice()) 
	                    && !"0".equals(stock.getPrice().trim())) {
	                priceStr = stock.getPrice().trim();
	            } 
	            // 所有价格都无效时，设为默认值（避免存0）
	            else {
	                priceStr = "0.00"; // 或根据业务需求设为其他默认值
	                log.warn("库存[{}({})]无有效价格，设为默认值{}", stock.getName(), stock.getId(), priceStr);
	            }

	            // 3. 设置月末统计价格
	            ms.setPrice(priceStr);

	            // 4. 更新库存最新单价：复用原有stock对象，避免重复查询
	            // 仅当价格有效且与原值不同时才更新，减少数据库操作
	            if (!priceStr.equals(stock.getPrice())) {
	                stock.setPrice(priceStr);
	                this.stockService.save(stock);
	                log.info("更新库存[{}({})]单价为：{}", stock.getName(), stock.getId(), priceStr);
	            }

	            // 5. 保存月末统计记录
	            this.save(ms);

	        } catch (Exception e) {
	            // 单个库存处理失败不影响整体，记录日志
	            log.error("处理库存[{}({})]自动统计失败", stock.getName(), stock.getId(), e);
	        }
	    });
	}

	@Override
	public List<MonthEndStatistics> findMonthEndStatisticsByDate(String date) {
		Query query = new Query();
		query.addCriteria(Criteria.where("date").is(date));
		return this.find(query, MonthEndStatistics.class);
	}

	
	
}
