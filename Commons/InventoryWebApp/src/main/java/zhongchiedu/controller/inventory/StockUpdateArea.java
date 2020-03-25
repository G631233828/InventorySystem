package zhongchiedu.controller.inventory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.ProjectStockStatistics;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.GoodsStorageService;
import zhongchiedu.inventory.service.ProjectStockService;
import zhongchiedu.inventory.service.ProjectStockStatisticsService;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.StockStatisticsServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
@RequestMapping("update")
public class StockUpdateArea {

	private @Autowired StockServiceImpl stockService;

	private @Autowired AreaServiceImpl areaService;
	
	private @Autowired StockStatisticsServiceImpl stockStatisticsService;
	
	private @Autowired ProjectStockService projectStockService;
	
	private @Autowired ProjectStockStatisticsService projectStockStatisticsService;
	
	private @Autowired GoodsStorageService goodsStorageService;

	@GetMapping("stocks")
	@RequiresPermissions(value = "stock:list")
	public String list(@RequestParam(value = "id", defaultValue = "")String id) {
		if(Common.isEmpty(id)) {
			return null;
		}
		Area area = this.areaService.findOneById(id, Area.class);
		if(area == null) {
			return null;
		}
		List<Stock> list = this.stockService.find(new Query(), Stock.class);
		for(Stock st:list) {
			st.setArea(area);
			this.stockService.save(st);
		}
		
		List<StockStatistics> liststatic = this.stockStatisticsService.find(new Query(), StockStatistics.class);
		for(StockStatistics st:liststatic) {
			st.setArea(area);
			this.stockStatisticsService.save(st);
		}
		return "admin/stock/list";
	}

	@GetMapping("projectstocks")
	@RequiresPermissions(value = "stock:list")
	public String project(@RequestParam(value = "id", defaultValue = "")String id) {
		if(Common.isEmpty(id)) {
			return null;
		}
		Area area = this.areaService.findOneById(id, Area.class);
		if(area == null) {
			return null;
		}
		List<ProjectStock> list = this.projectStockService.find(new Query(), ProjectStock.class);
		for(ProjectStock st:list) {
			st.setArea(area);
			this.projectStockService.save(st);
		}
		
		List<ProjectStockStatistics> liststatic = this.projectStockStatisticsService.find(new Query(), ProjectStockStatistics.class);
		for(ProjectStockStatistics st:liststatic) {
			st.setArea(area);
			this.projectStockStatisticsService.save(st);
		}
		return "admin/stock/list";
	}
	
	
	
	@GetMapping("inventory")
	@RequiresPermissions(value = "projectStock:list")
	public String updateInventory(@RequestParam(value = "id", defaultValue = "")String id) {
		
		List<ProjectStock> list = this.projectStockService.find(new Query(), ProjectStock.class); 
		
		for(ProjectStock stock :list) {
			stock.setInventory(stock.getActualPurchaseQuantity()-stock.getNum());
			this.projectStockService.save(stock);
		}
	
		return "admin/stock/list";
	}
	
	
	
	
	@GetMapping("goods")
	@RequiresPermissions(value = "projectStock:list")
	public String goods(@RequestParam(value = "id", defaultValue = "")String id) {
		
		if(Common.isEmpty(id)) {
			return null;
		}
		Area area = this.areaService.findOneById(id, Area.class);
		if(area == null) {
			return null;
		}
		
		List<GoodsStorage> list = this.goodsStorageService.find(new Query(), GoodsStorage.class); 
		
		for(GoodsStorage goods :list) {
			goods.setArea(area);
			this.goodsStorageService.save(goods);
		}
	
		return "admin/stock/list";
	}
	
	
	
	
	
	
	
	

}
