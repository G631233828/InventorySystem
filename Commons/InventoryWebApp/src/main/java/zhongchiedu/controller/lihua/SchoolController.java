package zhongchiedu.controller.lihua;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.School;
import zhongchiedu.inventory.service.SchoolService;
import zhongchiedu.log.annotation.SystemControllerLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.Date;

@Controller
@Slf4j
public class SchoolController {

    @Autowired
    private SchoolService schoolService;

    /**
     * 学校列表页（分页查询）
     */
    @GetMapping("schools")
    @RequiresPermissions(value = "schools:list")
    @SystemControllerLog(description = "查询所有学校信息")
    public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                       Model model,
                       @RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize,
                       HttpSession session,
                       @ModelAttribute("errorImport") String errorImport,
                       @RequestParam(value = "search", defaultValue = "") String search) {
        model.addAttribute("errorImport", errorImport);
        Pagination<School> pagination = this.schoolService.findpagination(pageNo, pageSize, search);
        model.addAttribute("pageList", pagination);
        model.addAttribute("search", search);

        try {
            model.addAttribute("today", Common.getDateYMD(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        // ====================== 已修改路径 ======================
        return "admin/lihua/school/list";
    }

    /**
     * 跳转到学校添加页面
     */
    @GetMapping("/school")
    @RequiresPermissions(value = "schools:add")
    public String addPage(Model model) {
        return "admin/lihua/school/add";
    }

    /**
     * 新增学校提交
     */
    @PostMapping("/school")
    @RequiresPermissions(value = "schools:add")
    @SystemControllerLog(description = "添加学校信息")
    public String addSchool(@ModelAttribute("school") School school) {
        this.schoolService.saveOrUpdate(school);
        return "redirect:schools";
    }

    /**
     * 编辑学校提交
     */
    @PutMapping("/school")
    @RequiresPermissions(value = "schools:edit")
    @SystemControllerLog(description = "修改学校信息")
    public String edit(@ModelAttribute("school") School school) {
        this.schoolService.saveOrUpdate(school);
        return "redirect:schools";
    }

    /**
     * 跳转到学校编辑页面
     */
    @GetMapping("/school{id}")
    @RequiresPermissions(value = "schools:edit")
    @SystemControllerLog(description = "编辑学校信息")
    public String toeditPage(@PathVariable String id, Model model) {
        School school = this.schoolService.findOneById(id, School.class);
        model.addAttribute("school", school);
        return "admin/lihua/school/add";
    }

    /**
     * 学校导入模板下载
     */
    @RequestMapping(value = "/school/download")
    @SystemControllerLog(description = "下载学校信息导入模版")
    @RequiresPermissions(value = "schools:batch")
    public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String storeName = "学校信息模版.xlsx";
        String contentType = "application/octet-stream";
        String UPLOAD = "Templates/";
        FileOperateUtil.download(request, response, storeName, contentType, UPLOAD);
        return null;
    }

    /**
     * 学校Excel批量上传导入
     */
    @RequestMapping(value = "/school/upload")
    @SystemControllerLog(description = "批量导入学校信息")
    @RequiresPermissions(value = "schools:batch")
    public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
        log.info("开始上传学校信息文件");
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/schools");
        String error = this.schoolService.upload(request, session);
        attr.addFlashAttribute("errorImport", error);
        return modelAndView;
    }

    /**
     * 获取学校导入进度
     */
    @RequestMapping(value = "/school/uploadprocess")
    @ResponseBody
    public Object process(HttpServletRequest request, HttpServletResponse response) {
        return this.schoolService.findproInfo(request);
    }

    /**
     * 禁用/启用学校
     */
    @RequestMapping(value = "/school/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    @SystemControllerLog(description = "禁用/启用学校信息")
    public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
        School school = this.schoolService.findOneById(id, School.class);
        if (school == null) {
            return BasicDataResult.build(400, "未找到该学校信息", null);
        }
        boolean newStatus = !school.getIsDisable();
        school.setIsDisable(newStatus);
        this.schoolService.save(school);
        return BasicDataResult.build(200, newStatus ? "禁用成功" : "启用成功", newStatus);
    }

    /**
     * 删除学校（逻辑删除）
     */
    @DeleteMapping("/school/{id}")
    @RequiresPermissions(value = "schools:delete")
    @SystemControllerLog(description = "删除学校信息")
    public String delete(@PathVariable String id) {
        log.info("删除学校信息：" + id);
        School school = this.schoolService.findOneById(id, School.class);
        if (school != null) {
            school.setIsDelete(true);
            this.schoolService.save(school);
        }
        log.info("删除学校信息：" + id + "成功");
        return "redirect:/schools";
    }
}