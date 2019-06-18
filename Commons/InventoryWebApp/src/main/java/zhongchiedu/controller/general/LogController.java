package zhongchiedu.controller.general;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.Role;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.Impl.LogServiceImpl;
import zhongchiedu.general.service.Impl.RoleServiceImpl;
import zhongchiedu.general.service.Impl.UserServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.log.pojo.Log;

@Controller
public class LogController {

	private @Autowired LogServiceImpl logService;

	@GetMapping("logs")
	// @RequiresPermissions(value = "user:list")
	// @SystemControllerLog(description = "查询所有用户")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session) {
		// 分页查询数据
		Pagination<Log> pagination;
		try {
			pagination = this.logService.findAllLog(pageNo, pageSize, "0");
			if (pagination == null)
				pagination = new Pagination<Log>();

			model.addAttribute("pageList", pagination);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "admin/log/logs";
	}

	@GetMapping("errors")
	// @RequiresPermissions(value = "user:list")
	// @SystemControllerLog(description = "查询所有用户")
	public String errors(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session) {
		// 分页查询数据
		Pagination<Log> pagination;
		try {
			pagination = this.logService.findAllLog(pageNo, pageSize, "1");
			if (pagination == null)
				pagination = new Pagination<Log>();

			model.addAttribute("pageList", pagination);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "admin/log/errors";
	}

}
