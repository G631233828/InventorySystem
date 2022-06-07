package zhongchiedu.controller.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.Contents.inventoryRoles;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;
import zhongchiedu.inventory.pojo.InventoryRole;
import zhongchiedu.inventory.service.Impl.InventoryRoleServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class InventoryRoleController {

	@Autowired
	private InventoryRoleServiceImpl inventoryRoleService;
	
	@Autowired
	private UserService userService;
	


	@GetMapping("inventoryRoles")
	@RequiresPermissions(value = "inventoryRole:list")
	@SystemControllerLog(description = "查询所有库存角色信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport) {
			model.addAttribute("errorImport", errorImport);
		Pagination<InventoryRole> pagination = this.inventoryRoleService.findpagination(pageNo, pageSize,"");
		model.addAttribute("pageList", pagination);
		return "admin/inventoryRole/list";
	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/inventoryRole")
	@RequiresPermissions(value = "inventoryRole:add")
	public String addPage(Model model) {
		
		List<User> users = this.userService.findAllUser();
		model.addAttribute("users", users);
		return "admin/inventoryRole/add";
	}

	@PostMapping("/inventoryRole")
	@RequiresPermissions(value = "inventoryRole:add")
	@SystemControllerLog(description = "添加库存角色")
	public String addUser(@ModelAttribute("inventoryRole") InventoryRole inventoryRole,String userIds) {
		this.inventoryRoleService.saveOrUpdate(inventoryRole,userIds);
		return "redirect:inventoryRoles";
	}

	@PutMapping("/inventoryRole")
	@RequiresPermissions(value = "inventoryRole:edit")
	@SystemControllerLog(description = "修改库存角色")
	public String edit(@ModelAttribute("inventoryRole") InventoryRole inventoryRole,String userIds) {
		this.inventoryRoleService.saveOrUpdate(inventoryRole,userIds);
		return "redirect:inventoryRoles";
	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/inventoryRole{id}")
	@RequiresPermissions(value = "inventoryRole:edit")
	@SystemControllerLog(description = "编辑库存角色")
	public String toeditPage(@PathVariable String id, Model model) {
		InventoryRole inventoryRole = this.inventoryRoleService.findOneById(id, InventoryRole.class);
		model.addAttribute("inventoryRole", inventoryRole);
		
		List<User> users = this.userService.findAllUser();
		List<User> getusers = users.stream().filter(user->!inventoryRole.getUsers().contains(user)).collect(Collectors.toList());
		model.addAttribute("users", getusers);
		return "admin/inventoryRole/add";

	}

	@DeleteMapping("/inventoryRole/{id}")
	@RequiresPermissions(value = "inventoryRole:delete")
	@SystemControllerLog(description = "删除库存角色")
	public String delete(@PathVariable String id) {
		log.info("删除库存角色" + id);
		this.inventoryRoleService.delete(id);
		log.info("删除库存角色" + id + "成功");
		return "redirect:/inventoryRoles";
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/inventoryRole/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name
		) {
		return this.inventoryRoleService.ajaxgetRepletes(name);
	}

	@RequestMapping(value = "/inventoryRole/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.inventoryRoleService.todisable(id);
	}
	
	


	

}
