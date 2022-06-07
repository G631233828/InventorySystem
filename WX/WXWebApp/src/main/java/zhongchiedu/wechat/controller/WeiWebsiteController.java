//package zhongchiedu.wechat.controller;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import zhongchiedu.commons.utils.Common;
//import zhongchiedu.framework.pagination.Pagination;
//import zhongchiedu.school.pojo.News;
//import zhongchiedu.school.pojo.WebMenu;
//import zhongchiedu.school.service.NewsService;
//import zhongchiedu.school.service.WebMenuService;
//
//@Controller
//@RequestMapping("/weiweb")
//public class WeiWebsiteController {
//
//	@Autowired
//	private WebMenuService webMenuService;
//
//	@Autowired
//	private NewsService newsService;
//
//	@RequestMapping("/index")
//	public String index(Model model) {
//		return "temp1/index";
//	}
//
//	@RequestMapping("/list/{menuId}")
//	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
//			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
//			@PathVariable(value = "menuId", required = true) String menuId) {
//		
//		WebMenu webMenu = this.webMenuService.findOneById(menuId, WebMenu.class);
//
//		List<WebMenu> webMenus = this.webMenuService.findWebMenuByFirstLevel(menuId);
//
//		Pagination<News> pageList = this.newsService.findNewsBySuperMenuId(menuId, pageNo, pageSize);
//		model.addAttribute("pageList", pageList);
//		model.addAttribute("webMenus", webMenus);
//		model.addAttribute("webMenu", webMenu);
//		return "temp1/list";
//	}
//
//	
//	
//	@RequestMapping(value = "/news/{newsId}")
//	public String findNews(Model model,@PathVariable(value="newsId",required = true)String newsId) {
//		
//		News news = this.newsService.findNewsById(newsId);
//		model.addAttribute("news", news);
//		return "temp1/detail";
//	}
//	
//	
//	
//	
//	
//	
//	
//	
//	
//}
