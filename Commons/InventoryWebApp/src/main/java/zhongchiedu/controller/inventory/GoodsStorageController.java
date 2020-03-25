package zhongchiedu.controller.inventory;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
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

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.Resource;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.CompanyServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class GoodsStorageController {

	@Autowired
	private GoodsStorageServiceImpl goodsStorageService;

	@Autowired
	private CompanyServiceImpl companyService;
	
	private @Autowired AreaServiceImpl areaService;

	@GetMapping("goodsStorages")
	@RequiresPermissions(value = "goodsStorage:list")
	@SystemControllerLog(description = "查询所有货架信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea) {
		Pagination<GoodsStorage> pagination = this.goodsStorageService.findpagination(pageNo, pageSize,searchArea);
		model.addAttribute("pageList", pagination);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		model.addAttribute("searchArea", searchArea);
		return "admin/goodsStorage/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/goodsStorage")
	@RequiresPermissions(value = "goodsStorage:add")
	public String addPage(Model model) {
		List<Companys> list = this.companyService.findAllCompany(false);
		model.addAttribute("companys", list);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		return "admin/goodsStorage/add";
	}

	@PostMapping("/goodsStorage")
	@RequiresPermissions(value = "goodsStorage:add")
	@SystemControllerLog(description = "添加货架")
	public String addUser(@ModelAttribute("goodsStorage") GoodsStorage goodsStorage) {
		this.goodsStorageService.saveOrUpdate(goodsStorage);
		return "redirect:goodsStorages";
	}

	@PutMapping("/goodsStorage")
	@RequiresPermissions(value = "goodsStorage:edit")
	@SystemControllerLog(description = "修改货架信息")
	public String edit(@ModelAttribute("goodsStorage") GoodsStorage goodsStorage) {
		this.goodsStorageService.saveOrUpdate(goodsStorage);
		return "redirect:goodsStorages";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/goodsStorage{id}")
	@RequiresPermissions(value = "goodsStorage:edit")
	@SystemControllerLog(description = "编辑货架信息")
	public String toeditPage(@PathVariable String id, Model model) {
		GoodsStorage goodsStorage = this.goodsStorageService.findOneById(id, GoodsStorage.class);
		model.addAttribute("goodsStorage", goodsStorage);
		List<Companys> list = this.companyService.findAllCompany(false);
		model.addAttribute("companys", list);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		return "admin/goodsStorage/add";

	}

	@DeleteMapping("/goodsStorage/{id}")
	@RequiresPermissions(value = "company:delete")
	@SystemControllerLog(description = "删除货架")
	public String delete(@PathVariable String id) {
		log.info("删除货架" + id);
		this.goodsStorageService.delete(id);
		log.info("删除货架" + id + "成功");
		return "redirect:/goodsStorages";
	}
	
	

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/goodsStorage/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "address", defaultValue = "") String address,
			@RequestParam(value = "shelfNumber", defaultValue = "") String shelfNumber,
			@RequestParam(value = "shelflevel", defaultValue = "") String shelflevel) {
		return this.goodsStorageService.ajaxgetRepletes(address, shelfNumber, shelflevel);
	}

	@RequestMapping(value = "/goodsStorage/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.goodsStorageService.todisable(id);
	}

}
