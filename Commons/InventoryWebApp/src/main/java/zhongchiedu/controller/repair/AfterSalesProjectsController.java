package zhongchiedu.controller.repair;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.AfterSalesProjects;
import zhongchiedu.inventory.service.AfterSalesProjectsService;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 售后项目保障 Controller
 * 参考 PnameController 实现规范，适配售后项目业务逻辑
 */
@Controller
@Slf4j
public class AfterSalesProjectsController {

    @Autowired
    private AfterSalesProjectsService afterSalesProjectsService;

    /**
     * 售后项目列表页（分页查询）
     */
    @GetMapping("afterSalesProjects")
    @RequiresPermissions(value = "afterSalesProjects:list")
    @SystemControllerLog(description = "查询所有售后项目保障信息")
    public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                       Model model,
                       @RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize,
                       HttpSession session,
                       @ModelAttribute("errorImport") String errorImport,
                       @RequestParam(value = "search", defaultValue = "") String search) {
        model.addAttribute("errorImport", errorImport);
        // 分页查询售后项目
        Pagination<AfterSalesProjects> pagination = this.afterSalesProjectsService.findpagination(pageNo, pageSize, search);
        model.addAttribute("pageList", pagination);
        model.addAttribute("search", search);

        try {
			model.addAttribute("today",Common.getDateYMD(new Date()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return "admin/afterSalesProjects/list";
    }

    /**
     * 跳转到售后项目添加页面
     */
    @GetMapping("/afterSalesProject")
    @RequiresPermissions(value = "afterSalesProjects:add")
    public String addPage(Model model) {
        // 若售后项目需关联其他实体（如客户/学校），可在此查询并传入
        // 示例：List<School> schoolList = schoolService.findAll(false);
        // model.addAttribute("schools", schoolList);
        return "admin/afterSalesProjects/add";
    }

    /**
     * 新增售后项目提交
     */
    @PostMapping("/afterSalesProject")
    @RequiresPermissions(value = "afterSalesProjects:add")
    @SystemControllerLog(description = "添加售后项目保障信息")
    public String addAfterSalesProject(@ModelAttribute("afterSalesProjects") AfterSalesProjects afterSalesProjects) {
        this.afterSalesProjectsService.saveOrUpdate(afterSalesProjects);
        return "redirect:afterSalesProjects";
    }

    /**
     * 编辑售后项目提交
     */
    @PutMapping("/afterSalesProject")
    @RequiresPermissions(value = "afterSalesProjects:edit")
    @SystemControllerLog(description = "修改售后项目保障信息")
    public String edit(@ModelAttribute("afterSalesProjects") AfterSalesProjects afterSalesProjects) {
        this.afterSalesProjectsService.saveOrUpdate(afterSalesProjects);
        return "redirect:afterSalesProjects";
    }

    /**
     * 跳转到售后项目编辑页面
     */
    @GetMapping("/afterSalesProject{id}")
    @RequiresPermissions(value = "afterSalesProjects:edit")
    @SystemControllerLog(description = "编辑售后项目保障信息")
    public String toeditPage(@PathVariable String id, Model model) {
        AfterSalesProjects afterSalesProjects = this.afterSalesProjectsService.findOneById(id, AfterSalesProjects.class);
        model.addAttribute("afterSalesProjects", afterSalesProjects);
        // 若需回显关联实体数据，可在此查询并传入
        // 示例：Object[] relateIds = afterSalesProjectsService.xxxIds(afterSalesProjects);
        // model.addAttribute("ids", relateIds);
        return "admin/afterSalesProjects/add";
    }

    /**
     * 售后项目导入模板下载
     */
    @RequestMapping(value = "/afterSalesProject/download")
    @SystemControllerLog(description = "下载售后项目保障信息导入模版")
    @RequiresPermissions(value = "afterSalesProjects:batch")
    public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String storeName = "售后项目保障模版.xlsx";
        String contentType = "application/octet-stream";
        String UPLOAD = "Templates/";
        FileOperateUtil.download(request, response, storeName, contentType, UPLOAD);
        return null;
    }

    /**
     * 售后项目Excel批量上传导入
     */
    @RequestMapping(value = "/afterSalesProject/upload")
    @SystemControllerLog(description = "批量导入售后项目保障信息")
    @RequiresPermissions(value = "afterSalesProjects:batch")
    public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
        log.info("开始上传售后项目保障文件");
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/afterSalesProjects");
        // 调用服务层上传方法
        String error = this.afterSalesProjectsService.upload(request, session);
        attr.addFlashAttribute("errorImport", error);
        return modelAndView;
    }

    /**
     * 获取售后项目导入进度
     */
    @RequestMapping(value = "/afterSalesProject/uploadprocess")
    @ResponseBody
    public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return this.afterSalesProjectsService.findproInfo(request);
    }

    /**
     * 禁用/启用售后项目（可选扩展，参考Pname的disable逻辑）
     */
    @RequestMapping(value = "/afterSalesProject/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
//    @RequiresPermissions(value = "afterSalesProjects:disable")
    @SystemControllerLog(description = "禁用/启用售后项目保障信息")
    public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
        // 若Service层补充了todisable方法，可直接调用
        // return this.afterSalesProjectsService.todisable(id);
        
        // 临时返回默认结果（若暂未实现禁用逻辑）
        AfterSalesProjects project = this.afterSalesProjectsService.findOneById(id, AfterSalesProjects.class);
        if (project == null) {
            return BasicDataResult.build(400, "未找到该售后项目信息", null);
        }
        boolean newStatus = !project.getIsDisable();
        project.setIsDisable(newStatus);
        this.afterSalesProjectsService.save(project);
        return BasicDataResult.build(200, newStatus ? "禁用成功" : "启用成功", newStatus);
    }

    /**
     * 删除售后项目（逻辑删除，参考Pname的delete逻辑）
     */
    @DeleteMapping("/afterSalesProject/{id}")
    @RequiresPermissions(value = "afterSalesProjects:delete")
    @SystemControllerLog(description = "删除售后项目保障信息")
    public String delete(@PathVariable String id) {
        log.info("删除售后项目保障信息：" + id);
        AfterSalesProjects project = this.afterSalesProjectsService.findOneById(id, AfterSalesProjects.class);
        if (project != null) {
            project.setIsDelete(true);
            this.afterSalesProjectsService.save(project);
        }
        log.info("删除售后项目保障信息：" + id + "成功");
        return "redirect:/afterSalesProjects";
    }
    
//    @RequestMapping(value = "/afterSalesProject/getAfterSalesProjects", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    @SystemControllerLog(description = "禁用/启用售后项目保障信息")
//    public BasicDataResult getAfterSalesProjects() {
//      List<AfterSalesProjects> findAllinServiceProj = this.afterSalesProjectsService.findAllinServiceProj();
//      if(findAllinServiceProj.size()>0) {
//    	  return BasicDataResult.build(200, "获取在保项目成功", findAllinServiceProj);
//      }
//     return  BasicDataResult.build(400, "获取在保项目失败", null);
//    }
    
}