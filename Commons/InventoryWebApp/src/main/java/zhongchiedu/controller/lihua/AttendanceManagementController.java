	package zhongchiedu.controller.lihua;
	
	import java.io.UnsupportedEncodingException;
	import java.net.URLEncoder;
	import java.text.SimpleDateFormat;
	import java.util.List;
	import java.util.stream.Collectors;
	
	import javax.servlet.http.HttpServletResponse;
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
	
	import com.alibaba.excel.EasyExcel;
	
	import lombok.extern.slf4j.Slf4j;
	import zhongchiedu.common.utils.BasicDataResult;
	import zhongchiedu.common.utils.Common;
	import zhongchiedu.framework.pagination.Pagination;
	import zhongchiedu.inventory.Dto.AttendanceExportDTO;
	import zhongchiedu.inventory.pojo.AttendanceManagement;
	import zhongchiedu.inventory.service.AttendanceManagementService;
	import zhongchiedu.log.annotation.SystemControllerLog;
	
	@Controller
	@Slf4j
	public class AttendanceManagementController {
	
	    @Autowired
	    private AttendanceManagementService attendanceManagementService;
	
	    @Value("${upload-imgpath}")
	    private String imgPath;
	
	    @Value("${upload-dir}")
	    private String dir;
	
	    /**
	     * 签到记录列表（支持搜索 + 分页）
	     */
	    @GetMapping("/attendances")
	    @RequiresPermissions(value = "attendance:list")
	    @SystemControllerLog(description = "查询所有签到记录")
	    public String list(
	            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
	            @RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize,
	            @RequestParam(value = "name", defaultValue = "") String name,
	            @RequestParam(value = "startDate", defaultValue = "") String startDate,
	            @RequestParam(value = "endDate", defaultValue = "") String endDate,
	            Model model,
	            // 加一个 HttpSession 参数
	            HttpSession session) {

	        // 1. 调用 Service 分页查询（带搜索条件）
	        Pagination<AttendanceManagement> pagination = attendanceManagementService.findpagination(pageNo, pageSize, name, startDate, endDate);

	        model.addAttribute("pageList", pagination);

	        // 2. 把搜索条件回显到页面（防止分页后搜索条件丢失）
	        model.addAttribute("name", name);
	        model.addAttribute("startDate", startDate);
	        model.addAttribute("endDate", endDate);

	        // =============================================
	        // 【新增】把分页参数存入 Session，删除时直接用
	        // =============================================
	        session.setAttribute("pageNo", pageNo);
	        session.setAttribute("pageSize", pageSize);
	        session.setAttribute("search", name);

	        return "/admin/lihua/attendanceManagement/list";
	    }
	
	
	
	    /**
	     * 删除
	     */
	    @DeleteMapping("/attendance/{id}")
	    @RequiresPermissions(value = "attendance:delete")
	    @SystemControllerLog(description = "删除签到记录")
	    public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
	        attendanceManagementService.delete(id);
	
	        Integer pageNo = (Integer) session.getAttribute("pageNo");
	        Integer pageSize = (Integer) session.getAttribute("pageSize");
	        String search = (String) session.getAttribute("search");
	        return "redirect:/attendances?pageNo=" + pageNo + "&pageSize=" + pageSize + "&name="
	                + URLEncoder.encode(search, "UTF-8");
	    }
	    
	    
	    
	    /**
	     * 导出签到打卡记录
	     */
	    @GetMapping("/attendance/export")
	    @RequiresPermissions(value = "attendance:export")
	    @SystemControllerLog(description = "导出签到打卡数据")
	    public void export(
	            @RequestParam(required = false) String startTime,
	            @RequestParam(required = false) String endTime,
	            @RequestParam(required = false) String name,
	            HttpServletResponse response) throws Exception {
	
	        // 设置响应头
	        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	        response.setCharacterEncoding("utf-8");
	        String fileName = URLEncoder.encode("签到记录_" + System.currentTimeMillis(), "UTF-8");
	        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
	
	        // 查询导出数据
	        List<AttendanceManagement> attendanceList = this.attendanceManagementService.findExportData(startTime, endTime, name);
	
	
	        // 转换 DTO
	        List<AttendanceExportDTO> exportList = attendanceList.stream().map(att -> {
	            AttendanceExportDTO dto = new AttendanceExportDTO();
	
	            dto.setName(att.getName());
	            dto.setSchoolName(att.getSchool() != null ? att.getSchool().getSchoolName() : "无");
	            dto.setSignTime(att.getSignTime() != null ? att.getSignTime() : "无");
	            dto.setProblems(att.getProblems() != null ? att.getProblems() : "无");
	            dto.setAddress(att.getAddress() != null ? att.getAddress() : "无");
	            dto.setClassRoom(att.getClassRoom()!=null ? att.getClassRoom():"无");
	            
	
	
	            return dto;
	        }).collect(Collectors.toList());
	
	        // 写出 Excel
	        EasyExcel.write(response.getOutputStream(), AttendanceExportDTO.class)
	                .sheet("签到记录")
	                .doWrite(exportList);
	    }
	    
	    
	    
	    
	    
	    
	
	}