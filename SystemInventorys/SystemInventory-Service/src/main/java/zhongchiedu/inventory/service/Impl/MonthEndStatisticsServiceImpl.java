package zhongchiedu.inventory.service.Impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	            // ========== 1. 计算综合平均价格（QC）：用于月末统计主价格 ==========
	            BigDecimal averagePrice = null;
	            try {
	                averagePrice = stockStatisticsService.getQC(stock.getId(), TimeRangeType.CURRENT_MONTH, date);
	            } catch (Exception e) {
	                log.error("计算库存[{}({})]综合平均价格失败", stock.getName(), stock.getId(), e);
	            }

	            // ========== 2. 计算当月入库平均单价：用于inprice字段 ==========
	            BigDecimal inPrice = BigDecimal.ZERO; // 初始化默认值
	            try {
	                // 调用方法获取当月入库平均单价
	                inPrice = this.stockStatisticsService.calculateAveragePriceByStockId(stock.getId(), TimeRangeType.CURRENT_MONTH, date);
	                // 统一格式：保留2位小数（避免多位小数或科学计数法）
	                inPrice = inPrice.setScale(2, RoundingMode.HALF_UP);
	                log.debug("库存[{}({})]当月入库平均单价计算结果：{}", stock.getName(), stock.getId(), inPrice);
	            } catch (Exception e) {
	                log.error("计算库存[{}({})]当月入库平均单价失败", stock.getName(), stock.getId(), e);
	                // 异常时仍保留默认值0.00，避免空值
	                inPrice = new BigDecimal("0.00");
	            }

	            // ========== 3. 价格处理：多层非空/合法校验 ==========
	            // 3.1 处理主价格（price字段）
	            String priceStr = "0.00"; // 默认值
	            // 优先使用综合平均价格
	            if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) > 0) {
	                priceStr = averagePrice.setScale(2, RoundingMode.HALF_UP).toPlainString();
	            }
	            // 综合价格无效时，使用库存原有价格
	            else if (Common.isNotEmpty(stock.getPrice()) && !"0".equals(stock.getPrice().trim())) {
	                // 校验并转换原有价格为标准格式（避免格式不统一）
	                try {
	                    priceStr = new BigDecimal(stock.getPrice().trim()).setScale(2, RoundingMode.HALF_UP).toPlainString();
	                } catch (NumberFormatException e) {
	                    log.warn("库存[{}({})]原有价格格式非法({})，使用默认值0.00", stock.getName(), stock.getId(), stock.getPrice());
	                    priceStr = "0.00";
	                }
	            }
	            // 所有价格无效时，保留默认值并记录日志
	            else {
	                log.warn("库存[{}({})]无有效综合价格，设为默认值{}", stock.getName(), stock.getId(), priceStr);
	            }

	            // 3.2 处理入库单价（inprice字段）
	            String inPriceStr = inPrice.toPlainString();
	            // 入库单价为0时补充日志（非错误，仅提示）
	            if (inPrice.compareTo(BigDecimal.ZERO) == 0) {
	                log.info("库存[{}({})]当月无入库记录，入库单价设为{}", stock.getName(), stock.getId(), inPriceStr);
	            }

	            // ========== 4. 赋值价格字段 ==========
	            ms.setPrice(priceStr);       // 月末统计主价格
	            ms.setInprice(inPriceStr);  // 当月入库平均单价（新增字段）

	            // ========== 5. 更新库存最新单价 ==========
	            // 仅当价格有效且与原值不同时才更新，减少数据库操作
	            if (!priceStr.equals(stock.getPrice())) {
	                stock.setPrice(priceStr);
	                this.stockService.save(stock);
	                log.info("更新库存[{}({})]单价为：{}", stock.getName(), stock.getId(), priceStr);
	            }

	            // ========== 6. 保存月末统计记录 ==========
	            this.save(ms);
	            log.debug("库存[{}({})]月末统计记录保存完成，入库单价：{}，月末单价：{}", 
	                    stock.getName(), stock.getId(), inPriceStr, priceStr);

	        } catch (Exception e) {
	            // 单个库存处理失败不影响整体，记录完整异常日志
	            log.error("处理库存[{}({})]自动统计失败", stock.getName(), stock.getId(), e);
	        }
	    });
	}
//	public void automaticStatistics(String date) {
//	    // 拿到所有库存的数据
//	    List<Stock> findAllStock = this.stockService.findAllStock();
//	    // 空集合防护
//	    if (Common.isEmpty(findAllStock)) {
//	        log.warn("自动统计库存：未查询到任何库存数据，跳过本次统计");
//	        return;
//	    }
//
//	    findAllStock.forEach(stock -> {
//	        try {
//	            log.info("记录库存设备{},数量{}", stock.getName(), stock.getInventory());
//	            MonthEndStatistics ms = new MonthEndStatistics();
//	            ms.setMonthEndStockNum(stock.getInventory());//库存数量
//	            ms.setDate(Common.isEmpty(date) ? Common.fromDateYMD() : date);
//	            ms.setStockName(stock.getName());
//	            ms.setStockModel(stock.getModel());
//	            // 供应商/区域非空保护：避免 NPE
//	            ms.setStockSuppier(stock.getSupplier() != null ? stock.getSupplier().getName() : "未知供应商");
//	            ms.setStockArea(stock.getArea() != null ? stock.getArea().getName() : "未知区域");
//	            ms.setStock(stock);
//
//	            // 1. 计算平均价格（带异常捕获）
//	            BigDecimal averagePrice = null;
//	            try {
//	                averagePrice = stockStatisticsService.getQC(stock.getId(), TimeRangeType.CURRENT_MONTH,date);
//	            } catch (Exception e) {
//	                log.error("计算库存[{}({})]平均价格失败", stock.getName(), stock.getId(), e);
//	            }
//
//	            // 2. 价格处理：多层非空/合法校验，避免空值/0值覆盖
//	            String priceStr = null;
//	            // 优先使用计算出的平均价格
//	            if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) > 0) {
//	                priceStr = averagePrice.toPlainString();
//	            } 
//	            // 平均价格无效时，使用库存原有价格（需校验）
//	            else if (Common.isNotEmpty(stock.getPrice()) 
//	                    && !"0".equals(stock.getPrice().trim())) {
//	                priceStr = stock.getPrice().trim();
//	            } 
//	            // 所有价格都无效时，设为默认值（避免存0）
//	            else {
//	                priceStr = "0.00"; // 或根据业务需求设为其他默认值
//	                log.warn("库存[{}({})]无有效价格，设为默认值{}", stock.getName(), stock.getId(), priceStr);
//	            }
//
//	            // 3. 设置月末统计价格
//	            ms.setPrice(priceStr);
//	            
//	            
//	            BigDecimal calculateAveragePriceByStockId = this.stockStatisticsService.calculateAveragePriceByStockId(stock.getId(), TimeRangeType.CURRENT_MONTH, date);
//	            
//	            
//
//	            // 4. 更新库存最新单价：复用原有stock对象，避免重复查询
//	            // 仅当价格有效且与原值不同时才更新，减少数据库操作
//	            if (!priceStr.equals(stock.getPrice())) {
//	                stock.setPrice(priceStr);
//	                this.stockService.save(stock);
//	                log.info("更新库存[{}({})]单价为：{}", stock.getName(), stock.getId(), priceStr);
//	            }
//
//	            // 5. 保存月末统计记录
//	            this.save(ms);
//
//	        } catch (Exception e) {
//	            // 单个库存处理失败不影响整体，记录日志
//	            log.error("处理库存[{}({})]自动统计失败", stock.getName(), stock.getId(), e);
//	        }
//	    });
//	}

	@Override
	public List<MonthEndStatistics> findMonthEndStatisticsByDate(String date) {
		Query query = new Query();
		query.addCriteria(Criteria.where("date").is(date));
		return this.find(query, MonthEndStatistics.class);
	}

	
	
}
