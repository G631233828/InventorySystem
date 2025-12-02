package zhongchiedu.controller.repair;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class WxBindingController {

	@Autowired
	private WxBindingService wxBindingService;

	@GetMapping("/wxBinding")
	@RequiresPermissions(value = "wxBinding:list")
	@SystemControllerLog(description = "查询所有绑定信息")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session) {
		Pagination<WxBinding> pagination = this.wxBindingService.findpagination(pageNo, pageSize);
		model.addAttribute("pageList", pagination);

		return "wechat/wxbinding/list";
	}

	@DeleteMapping("/wxBinding/{id}")
	@RequiresPermissions(value = "wxBinding:delete")
	@SystemControllerLog(description = "删除报修信息")
	public String delete(@PathVariable String id) {
		log.info("删除" + id);
		this.wxBindingService.delete(id);
		log.info("删除" + id + "成功");
		return "redirect:wxRepairs";
	}

	@RequiresPermissions("wxBinding:audit")
	@PostMapping("/wxBinding/audit/{id}")
	@ResponseBody // 确保返回JSON（如果未全局配置）
	public BasicDataResult audit(@PathVariable("id") String id, 
	                             @RequestParam("status") Integer status,
	                             HttpServletRequest request) {
	    try {
	        // 校验参数合法性
	        if (id == null || id.trim().isEmpty() || status == null) {
	            return BasicDataResult.build(400, "参数不能为空", null);
	        }
	        if (!(status == 2 || status == 3)) {
	            return BasicDataResult.build(400, "审核状态只能是2（通过）或3（拒绝）", null);
	        }

	        // 调用业务层处理审核逻辑
	        boolean result = wxBindingService.auditWxBinding(id, status);

	        if (result) {
	            String statusDesc = status == 2 ? "审核通过" : "审核拒绝";
	            log.info("微信绑定记录[{}]审核操作成功，状态更新为：{}", id, statusDesc);
	            return BasicDataResult.ok(statusDesc + "操作成功！"); // 简化返回（如果BasicDataResult有ok方法）
	        } else {
	            return BasicDataResult.build(500, "审核操作失败，请重试！", null);
	        }
	    } catch (IllegalArgumentException e) {
	        log.error("微信绑定记录[{}]审核参数异常：{}", id, e.getMessage());
	        return BasicDataResult.build(400, e.getMessage(), null);
	    } catch (RuntimeException e) {
	        log.error("微信绑定记录[{}]审核业务异常：{}", id, e.getMessage());
	        return BasicDataResult.build(400, e.getMessage(), null); // 业务异常返回400，前端友好提示
	    } catch (Exception e) {
	        log.error("微信绑定记录[{}]审核系统异常", id, e);
	        return BasicDataResult.build(500, "系统异常，请联系管理员！", null);
	    }
	}

}
