package zhongchiedu.controller.lihua;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.SignTask;
import zhongchiedu.inventory.service.SignTaskService;
import zhongchiedu.log.annotation.SystemControllerLog;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.Date;

@Controller
@Slf4j
public class SignTaskController {

    @Autowired
    private SignTaskService signTaskService;

    // ================== 列表页 ==================
    @GetMapping("signTasks")
    @RequiresPermissions(value = "signTasks:list")
    @SystemControllerLog(description = "查询所有签到任务")
    public String list(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            Model model,
            @RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize,
            HttpSession session,
            @RequestParam(value = "search", defaultValue = "") String search) {

        Pagination<SignTask> pagination = signTaskService.findpagination(pageNo, pageSize, search);
        model.addAttribute("pageList", pagination);
        model.addAttribute("search", search);

        try {
            model.addAttribute("today", Common.getDateYMD(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "admin/lihua/signTask/list";
    }

    // ================== 去添加页面 ==================
    @GetMapping("/signTask")
    @RequiresPermissions(value = "signTasks:add")
    public String addPage(Model model) {
        return "admin/lihua/signTask/add";
    }

    // ================== 添加提交 ==================
    @PostMapping("/signTask")
    @RequiresPermissions(value = "signTasks:add")
    @SystemControllerLog(description = "添加签到任务")
    public String add(@ModelAttribute("signTask") SignTask signTask) {
        signTaskService.saveOrUpdate(signTask);
        return "redirect:signTasks";
    }

    // ================== 修改提交 ==================
    @PutMapping("/signTask")
    @RequiresPermissions(value = "signTasks:edit")
    @SystemControllerLog(description = "修改签到任务")
    public String edit(@ModelAttribute("signTask") SignTask signTask) {
        signTaskService.saveOrUpdate(signTask);
        return "redirect:signTasks";
    }

    // ================== 去编辑页面 ==================
    @GetMapping("/signTask{id}")
    @RequiresPermissions(value = "signTasks:edit")
    @SystemControllerLog(description = "编辑签到任务")
    public String toEdit(@PathVariable String id, Model model) {
        SignTask task = signTaskService.findOneById(id, SignTask.class);
        model.addAttribute("signTask", task);
        return "admin/lihua/signTask/add";
    }

    // ================== 禁用/启用 ==================
    @PostMapping(value = "/signTask/disable", produces = "application/json;charset=UTF-8")
    @ResponseBody
    @SystemControllerLog(description = "禁用/启用签到任务")
    public BasicDataResult disable(@RequestParam String id) {
        SignTask task = signTaskService.findOneById(id, SignTask.class);
        if (task == null) return BasicDataResult.build(400, "任务不存在", null);

        task.setIsDisable(!task.getIsDisable());
        signTaskService.save(task);
        return BasicDataResult.build(200, task.getIsDisable() ? "禁用成功" : "启用成功", null);
    }

    // ================== 删除 ==================
    @DeleteMapping("/signTask/{id}")
    @RequiresPermissions(value = "signTasks:delete")
    @SystemControllerLog(description = "删除签到任务")
    public String delete(@PathVariable String id) {
        SignTask task = signTaskService.findOneById(id, SignTask.class);
        if (task != null) {
            task.setIsDelete(true);
            signTaskService.save(task);
        }
        return "redirect:/signTasks";
    }
}