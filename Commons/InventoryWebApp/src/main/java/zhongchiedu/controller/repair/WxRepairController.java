package zhongchiedu.controller.repair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class WxRepairController {

    @Autowired
    private WxRepairService wxRepairService;

    @Autowired
    private WxBindingService wxBindingService;

    @Value("${upload-imgpath}")
    private String imgPath;

    @Value("${upload-dir}")
    private String dir;

    /**
     * 报修单列表查询（分页+条件）
     */
    @GetMapping("/wxRepairs")
    @RequiresPermissions(value = "wxRepair:list")
    @SystemControllerLog(description = "查询所有报修单")
    public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
                       @RequestParam(value = "search", defaultValue = "") String search,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "workerId", required = false) String workerId,
                       HttpSession session, Model model) {
        // 分页查询
        Pagination<WxRepair> pagination = wxRepairService.findpagination(pageNo, pageSize);
        
        List<WxBinding> findBindingsByPersonnelType = this.wxBindingService.findBindingsByPersonnelType(PersonnelType.CONSTRUCTION_TEAM);
        
        model.addAttribute("pageList", pagination);

        // 回显参数
        model.addAttribute("pageNo", pageNo);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("workerId", workerId);
        model.addAttribute("workerList", findBindingsByPersonnelType);

        // 保存session参数
        session.setAttribute("pageNo", pageNo);
        session.setAttribute("pageSize", pageSize);
        session.setAttribute("search", search);
        session.setAttribute("status", status);
        session.setAttribute("workerId", workerId);

        return "/wechat/repair/list";
    }

   

    /**
     * 删除报修单
     */
    @DeleteMapping("/wxRepair/{id}")
    @RequiresPermissions(value = "wxRepair:delete")
    @SystemControllerLog(description = "删除报修单")
    public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
        wxRepairService.delete(id);

        // 重定向回列表页
        Integer pageNo = (Integer) session.getAttribute("pageNo");
        Integer pageSize = (Integer) session.getAttribute("pageSize");
        String search = (String) session.getAttribute("search");
        return "redirect:/wxRepairs?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
                + URLEncoder.encode(search, "UTF-8");
    }

    /**
     * 分配维修人员
     */
    @PostMapping("/wxRepair/assignWorker")
    @RequiresPermissions(value = "wxRepair:assign")
    @SystemControllerLog(description = "分配维修人员")
    @ResponseBody
    public BasicDataResult assignWorker(@RequestParam("repairId") String repairId,
                                        @RequestParam("workerId") String workerId) {
        try {
            WxRepair wxRepair = wxRepairService.assignWorkerToRepair(repairId, workerId);
            if (Objects.nonNull(wxRepair)) {
                return BasicDataResult.build(200, "维修人员分配成功", wxRepair);
            } else {
                return BasicDataResult.build(400, "维修人员分配失败", null);
            }
        } catch (Exception e) {
            log.error("分配维修人员失败：", e);
            return BasicDataResult.build(500, "系统异常", null);
        }
    }



}