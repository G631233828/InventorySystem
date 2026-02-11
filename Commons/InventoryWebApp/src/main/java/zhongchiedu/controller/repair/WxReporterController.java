package zhongchiedu.controller.repair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.bson.types.ObjectId;
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
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.WxReporterService;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class WxReporterController {

    @Autowired
    private WxReporterService wxReporterService;

    /**
     * 报修人列表查询（分页+搜索）
     * 适配现有Service的分页逻辑，支持搜索过滤
     */
    @GetMapping("/wxReporters")
    @RequiresPermissions(value = "wxReporter:list")
    @SystemControllerLog(description = "查询所有报修人信息")
    public String list(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "search", defaultValue = "") String search,
            HttpSession session, Model model) {
        
        // 1. 基础分页查询
        Pagination<WxReporter> pagination = wxReporterService.findpagination(pageNo, pageSize);
           
        // 3. 回显参数
        model.addAttribute("pageList", pagination);
        model.addAttribute("pageNo", pageNo);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("search", search);

        return "/wechat/reporter/list";
    }

    /**
     * 跳转到报修人编辑界面（参考Area模块，使用路径参数）
     */
    @GetMapping("/wxReporter{id}")  // 路径参数：/wxReporter/698004af1211cc6564b99e98
    @RequiresPermissions(value = "wxReporter:edit")  // 仅保留编辑权限，对齐Area模块
    @SystemControllerLog(description = "编辑报修人")  // 新增日志注解，对齐Area模块
    public String toeditPage(@PathVariable String id, Model model, HttpServletRequest request) {
        try {
            // 1. 校验ID格式（MongoDB ObjectId）
            new org.bson.types.ObjectId(id);
            
            // 2. 查询报修人信息（参考Area模块的查询方式）
            WxReporter wxReporter = this.wxReporterService.findOneById(id, WxReporter.class);
            
            // 3. 校验数据有效性（排除已删除/不存在的数据）
            if (wxReporter == null || Boolean.TRUE.equals(wxReporter.getIsDelete())) {
                request.setAttribute("errorMsg", "报修人信息不存在或已删除");
                return "redirect:/wxReporters";  // 跳回列表页
            }
            
            // 4. 回显数据到页面
            model.addAttribute("wxReporter", wxReporter);
            
            // 5. 返回编辑页面（参考Area模块，编辑/新增复用同一页面）
            return "/wechat/reporter/edit";
        } catch (IllegalArgumentException e) {
            // ID格式错误处理
            request.setAttribute("errorMsg", "报修人ID格式错误");
            return "redirect:/wxReporters";
        } catch (Exception e) {
            // 通用异常处理
            log.error("编辑报修人失败，ID：{}", id, e);
            request.setAttribute("errorMsg", "编辑报修人失败：" + e.getMessage());
            return "redirect:/wxReporters";
        }
    }

    /**
     * 新增报修人跳转（无ID，对齐Area模块新增逻辑）
     */
    @GetMapping("/wxReporter")
    @RequiresPermissions(value = "wxReporter:add")
    @SystemControllerLog(description = "新增报修人")
    public String toAddPage() {
        // 新增页面复用编辑页面，无数据回显
        return "/wechat/reporter/edit";
    }

    /**
     * 保存/更新报修人信息
     * 适配Service的saveOrUpdate逻辑（基于openId判断新增/更新）
     */
    @PostMapping("/wxReporter/saveOrUpdate")
    @RequiresPermissions(value = {"wxReporter:add", "wxReporter:edit"})
    @SystemControllerLog(description = "保存/更新报修人信息")
    @ResponseBody
    public BasicDataResult saveOrUpdate(WxReporter wxReporter) {
        try {
            // 1. 基础参数校验
            if (Common.isEmpty(wxReporter.getUserName())) {
                return BasicDataResult.build(500, "报修人姓名不能为空", null);
            }
            if (Common.isEmpty(wxReporter.getContactNumber())) {
                return BasicDataResult.build(500, "联系电话不能为空", null);
            }
            if (Common.isEmpty(wxReporter.getSchoolName())) {
                return BasicDataResult.build(500, "学校名称不能为空", null);
            }
            // 手机号格式校验
            if (!wxReporter.getContactNumber().matches("^1[3-9]\\d{9}$")) {
                return BasicDataResult.build(500, "手机号格式不正确", null);
            }
            
            // 2. 调用Service的saveOrUpdate方法（自动处理openId唯一性）
            WxReporter result = wxReporterService.saveOrUpdate(wxReporter);
            
            // 3. 返回结果
            String msg = Common.isEmpty(result.getId()) ? "新增成功" : "编辑成功";
            return BasicDataResult.build(200, msg, result);
        } catch (Exception e) {
            log.error("保存报修人信息失败：", e);
            return BasicDataResult.build(500, "操作失败：" + e.getMessage(), null);
        }
    }

    /**
     * 删除报修人信息（逻辑删除，适配Service的delete方法）
     */
    @DeleteMapping("/wxReporter/{id}")
    @RequiresPermissions(value = "wxReporter:delete")
    @SystemControllerLog(description = "删除报修人信息")
    public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
        // 调用Service的删除方法（逻辑删除：isDelete=true）
        String result = wxReporterService.delete(id);
        
        // 重定向回列表页
        Integer pageNo = (Integer) session.getAttribute("pageNo");
        Integer pageSize = (Integer) session.getAttribute("pageSize");
        String search = (String) session.getAttribute("search");
        
        return "redirect:/wxReporters?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
                + URLEncoder.encode(search, "UTF-8");
    }

    /**
     * 根据openId查询报修人（供前端/其他接口调用）
     */
    @GetMapping("/wxReporter/findByOpenId")
    @ResponseBody
    public BasicDataResult findByOpenId(@RequestParam String openId) {
        if (Common.isEmpty(openId)) {
            return BasicDataResult.build(500, "openId不能为空", null);
        }
        WxReporter wxReporter = wxReporterService.findWxReporterByOpenId(openId);
        return BasicDataResult.build(200, "查询成功", wxReporter);
    }
}