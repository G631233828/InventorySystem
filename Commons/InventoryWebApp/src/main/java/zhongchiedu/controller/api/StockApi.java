package zhongchiedu.controller.api;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;


@Controller
@Slf4j
@RequestMapping("/api")
public class StockApi {
	
	private @Autowired StockServiceImpl stockService;
	
	
	
//	@GetMapping("/stocks")
//	@SystemControllerLog(description = "调用库存api")
//	@ResponseBody
//	public Pagination<Stock> list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
//			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
//			@RequestParam(value = "search", defaultValue = "") String search) {
//		Pagination<Stock> pagination = this.stockService.findpagination(pageNo, pageSize, search);
//		model.addAttribute("pageList", pagination);
//		model.addAttribute("pageSize", pageSize);
//		model.addAttribute("search", search);
//
//		return pagination;
//	}

}
