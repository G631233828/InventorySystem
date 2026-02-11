//package zhongchiedu.controller.inventory;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//
//import org.apache.shiro.authz.annotation.RequiresPermissions;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.ModelAttribute;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.servlet.ModelAndView;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import lombok.extern.slf4j.Slf4j;
//import zhongchiedu.common.utils.BasicDataResult;
//import zhongchiedu.framework.pagination.Pagination;
//import zhongchiedu.general.pojo.MultiMedia;
//import zhongchiedu.general.service.Impl.MultiMediaServiceImpl;
//import zhongchiedu.log.annotation.SystemControllerLog;
//
///**
// * 多媒体资源管理控制器
// * 功能：列表查询、添加、编辑、删除、禁用/启用、详情查看
// */
//@Controller
//@Slf4j
//public class MultiMediaController {
//
//    @Autowired
//    private MultiMediaServiceImpl multiMediaService;
//
//    /**
//     * 多媒体资源列表页
//     */
//    @GetMapping("multimedias")
////    @RequiresPermissions(value = "multimedia:list")
//    @SystemControllerLog(description = "查询所有多媒体资源信息")
//    public String list(
//            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
//            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
//            @RequestParam(value = "fileType", required = false) String fileType, // 按文件类型筛选
//            Model model,
//            HttpSession session,
//            @ModelAttribute("errorMsg") String errorMsg) {
//
//        model.addAttribute("errorMsg", errorMsg);
//        // 构建查询条件
//        Pagination<MultiMedia> pagination = multiMediaService.findMultiMediaPagination(pageNo, pageSize, fileType);
//        model.addAttribute("pageList", pagination);
//        model.addAttribute("selectedFileType", fileType); // 回显筛选条件
//        return "admin/multimedia/list";
//    }
//
//    /**
//     * 跳转到编辑页面
//     */
//    @GetMapping("/{id}")
//    @RequiresPermissions(value = "multimedia:edit")
//    @SystemControllerLog(description = "编辑多媒体资源")
//    public String toEditPage(@PathVariable String id, Model model) {
//        MultiMedia multiMedia = multiMediaService.findOneById(id, MultiMedia.class);
//        model.addAttribute("multimedia", multiMedia);
//        return "admin/multimedia/edit";
//    }
//
//    /**
//     * 编辑多媒体资源（仅修改标题、作者等基础信息）
//     */
//    @PutMapping
////    @RequiresPermissions(value = "multimedia:edit")
//    @SystemControllerLog(description = "修改多媒体资源信息")
//    public String edit(MultiMedia multiMedia, RedirectAttributes attr) {
//        try {
//            multiMediaService.editMultiMedia(multiMedia.getId(), multiMedia);
//            attr.addFlashAttribute("errorMsg", "编辑成功");
//        } catch (Exception e) {
//            log.error("编辑多媒体资源失败", e);
//            attr.addFlashAttribute("errorMsg", "编辑失败：" + e.getMessage());
//        }
//        return "redirect:/multimedia/list";
//    }
//
//    /**
//     * 删除多媒体资源（逻辑删除+物理删除文件）
//     */
//    @DeleteMapping("/{id}/{type}")
////    @RequiresPermissions(value = "multimedia:delete")
//    @SystemControllerLog(description = "删除多媒体资源")
//    public String delete(@PathVariable String id, @PathVariable String type, RedirectAttributes attr) {
//        try {
//            multiMediaService.deleteMultiMedia(id, type);
//            attr.addFlashAttribute("errorMsg", "删除成功");
//        } catch (Exception e) {
//            log.error("删除多媒体资源失败", e);
//            attr.addFlashAttribute("errorMsg", "删除失败：" + e.getMessage());
//        }
//        return "redirect:/multimedia/list";
//    }
//
//    /**
//     * 禁用/启用多媒体资源
//     */
//    @RequestMapping(value = "/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    @ResponseBody
////    @RequiresPermissions(value = "multimedia:disable")
//    public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
//        if (id == null || id.isEmpty()) {
//            return BasicDataResult.build(400, "无法禁用，请求参数为空!", null);
//        }
//
//        MultiMedia multiMedia = multiMediaService.findOneById(id, MultiMedia.class);
//        if (multiMedia == null) {
//            return BasicDataResult.build(400, "无法获取到资源信息，该资源可能已被删除", null);
//        }
//
//        // 切换禁用状态
//        multiMedia.setIsDisable(!multiMedia.getIsDisable());
//        multiMediaService.save(multiMedia);
//
//        String msg = multiMedia.getIsDisable() ? "禁用成功" : "启用成功";
//        return BasicDataResult.build(200, msg, multiMedia.getIsDisable());
//    }
//
//    /**
//     * 预览资源（图片/视频）
//     */
//    @GetMapping("/preview/{id}")
////    @RequiresPermissions(value = "multimedia:preview")
//    public String preview(@PathVariable String id, Model model) {
//        MultiMedia multiMedia = multiMediaService.findOneById(id, MultiMedia.class);
//        model.addAttribute("multimedia", multiMedia);
//        return "admin/multimedia/preview";
//    }
//}