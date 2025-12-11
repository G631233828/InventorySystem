package zhongchiedu.controller.repair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import zhongchiedu.common.utils.enums.PersonJoinAuditStatusEnum;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.Dto.WxRepairExportDTO;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.inventory.service.WxReporterService;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.template.WxMsgPush;

@Controller
@Slf4j
public class WxRepairController {

    @Autowired
    private WxRepairService wxRepairService;

    @Autowired
    private WxBindingService wxBindingService;
    
    @Autowired
    private WxReporterService wxReporterService;

    @Value("${upload-imgpath}")
    private String imgPath;

    @Value("${upload-dir}")
    private String dir;
    
	@Value("${templateId5}")
	private String templateId5; // 维修订单模版

	@Value("${qrcode.weburl}")
	private String weburl;

	@Autowired
	private WxMsgPush wxMsgPush; 
    

	/**
	 * 报修单列表查询（分页+条件）
	 */
	@GetMapping("/wxRepairs") // 统一路径与前端匹配
	@RequiresPermissions(value = "wxRepair:list")
	@SystemControllerLog(description = "查询所有报修单")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
	                   @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
	                   @RequestParam(value = "search", defaultValue = "") String search,
	                   @RequestParam(value = "status", required = false) Integer status,
	                   @RequestParam(value = "urgencyLevel", required = false) String urgencyLevel,
	                   @RequestParam(value = "workerId", required = false) String workerId,
	                   HttpSession session, Model model) {
	    // 分页查询（传递所有检索条件）
	    Pagination<WxRepair> pagination = wxRepairService.findpagination(pageNo, pageSize, search, status, urgencyLevel, workerId);
	    
	    List<WxBinding> findBindingsByPersonnelType = this.wxBindingService.findBindingsByPersonnelType(PersonnelType.CONSTRUCTION_TEAM,PersonJoinAuditStatusEnum.APPROVED);
	    
	    model.addAttribute("pageList", pagination);

	    // 回显所有参数
	    model.addAttribute("pageNo", pageNo);
	    model.addAttribute("pageSize", pageSize);
	    model.addAttribute("search", search);
	    model.addAttribute("status", status);
	    model.addAttribute("urgencyLevel", urgencyLevel); // 新增紧急程度回显
	    model.addAttribute("workerId", workerId);
	    model.addAttribute("workerList", findBindingsByPersonnelType);

	    // 保存session参数
	    session.setAttribute("pageNo", pageNo);
	    session.setAttribute("pageSize", pageSize);
	    session.setAttribute("search", search);
	    session.setAttribute("status", status);
	    session.setAttribute("urgencyLevel", urgencyLevel); // 新增紧急程度session
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
            	

				// 分配成功 執行推送消息

				Map<String, String> map = new HashMap<>();
//    				map.put("thing4", wxRepair.getWxReporter().getSchoolName()+"校区："+wxRepair.getWxReporter().getCampus());
//    				map.put("thing5", wxRepair.getWxReporter().getUserName());
//    				map.put("time2", Common.getDateYMDHM(wxRepair.getExpectedVisitTime()));
//    				map.put("thing16", wxRepair.getUrgencyLevel());
//    				map.put("thing11", wxRepair.getFaultInformation());

				// 先处理 wxReporter 空值（核心：避免 wxReporter 为 null 导致后续调用抛空指针）
				WxReporter reporter = wxRepair.getWxReporter();
				if (reporter == null) {
					reporter = new WxReporter(); // 若为 null，创建空对象避免后续频繁判断
				}

				// 1. thing4：学校+校区（分别校验空值，避免拼接出"null校区：null"）
				String schoolName = Common.isNotEmpty(reporter.getSchoolName()) ? reporter.getSchoolName() : "未知学校";
				String campus = Common.isNotEmpty(reporter.getCampus()) ? reporter.getCampus() : "未知校区";
				map.put("thing4", schoolName + "校区：" + campus);

				// 2. thing5：报修人姓名（默认"未知报修人"）
				String userName = Common.isNotEmpty(reporter.getUserName()) ? reporter.getUserName() : "未知报修人";
				String contactNumber = Common.isNotEmpty(reporter.getContactNumber()) ? reporter.getContactNumber() : "";
				
				map.put("thing5", userName);

				// 3. time2：期望时间（日期可能为 null，默认"未知期望时间"）
				String expectedTime = (wxRepair.getExpectedVisitTime() != null)
						? Common.getDateYMD(wxRepair.getExpectedVisitTime())
						: "未知期望时间";
				map.put("time2", expectedTime);

				// 4. thing16：紧急程度（默认"普通"，和之前逻辑一致）
				String urgencyLevel = Common.isNotEmpty(wxRepair.getUrgencyLevel()) ? wxRepair.getUrgencyLevel() : "普通";
				map.put("thing16", urgencyLevel);

				// 5. thing11：故障描述（默认"无详细故障描述"，更贴合业务）
				String faultInfo = Common.isNotEmpty(wxRepair.getFaultInformation()) ? wxRepair.getFaultInformation()
						: "无详细故障描述";
				map.put("thing11", faultInfo);
				// 执行推送
				String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId5, wxRepair.getWorker().getOpenId(),
						weburl + "/wechatrp/findWxRepairByWorker/" + wxRepair.getId(), map);
				log.info("用户[{}]提交报修单成功，消息推送成功：{}", wxRepair.getWorker().getOpenId(), sendWxMessage);
            	
                return BasicDataResult.build(200, "维修人员分配成功", wxRepair);
            } else {
                return BasicDataResult.build(400, "维修人员分配失败", null);
            }
        } catch (Exception e) {
            log.error("分配维修人员失败：", e);
            return BasicDataResult.build(500, "系统异常", null);
        }
    }

    
    
    
    
    /**
     * 导出报修单数据
     */
    @GetMapping("/wxRepair/export")
    @RequiresPermissions(value = "wxRepair:export")
    @SystemControllerLog(description = "导出报修单数据")
    public void export(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String workerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String search,
            HttpServletResponse response) throws Exception {
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("报修单数据_" + System.currentTimeMillis(), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        
        // 查询导出数据
        List<WxRepair> repairList = wxRepairService.findExportData(startTime, endTime, workerId, status, search);
        
        // 转换为DTO
        List<WxRepairExportDTO> exportList = repairList.stream().map(repair -> {
            WxRepairExportDTO dto = new WxRepairExportDTO();
            
            dto.setWorkOrderNumber(repair.getWorkOrderNumber());
            dto.setUserName(repair.getWxReporter().getUserName());
            dto.setContactNumber(repair.getWxReporter().getContactNumber());
            dto.setFaultInformation(repair.getFaultInformation()!=null?repair.getFaultInformation():"无");
            dto.setSchoolName(repair.getWxReporter() != null ? repair.getWxReporter().getSchoolName() : "无");
            dto.setCampus(repair.getWxReporter() != null ? repair.getWxReporter().getCampus() : "无");
            dto.setSchoolAddress(repair.getWxReporter() != null ? repair.getWxReporter().getSchoolAddress() : "无");
            dto.setReportClassroomRepair(repair.getReportClassroomRepair());
            dto.setEquipmentRepair(repair.getEquipmentRepair());
            dto.setUrgencyLevel(repair.getUrgencyLevel());
            dto.setCompleteTime(repair.getCompleteTime()!=null?repair.getCompleteTime():"未完成");
            dto.setWorkDept(repair.getWorkDept()!=null?repair.getWorkDept():"无");
            dto.setDescription(repair.getDescription()!=null?repair.getDescription():"无");
            // 格式化日期
            if (repair.getCreateTime() != null) {
                dto.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(repair.getCreateTime()));
            }
            if (repair.getExpectedVisitTime() != null) {
                dto.setExpectedVisitTime(repair.getExpectedVisitTime());
            }
            
            // 状态描述
            switch (repair.getStatus()) {
                case 1: dto.setStatusDesc("待处理"); break;
                case 2: dto.setStatusDesc("已分配"); break;
                case 3: dto.setStatusDesc("处理中"); break;
                case 4: dto.setStatusDesc("已完成"); break;
                case 5: dto.setStatusDesc("已取消"); break;
                default: dto.setStatusDesc("未知");
            }
            
            dto.setWorkerName(repair.getWorker() != null ? repair.getWorker().getName() : "未分配");
            dto.setRepairContent(repair.getRepairContent() != null ? repair.getRepairContent() : "无");
            return dto;
        }).collect(Collectors.toList());
        
        // 写入Excel并返回
        EasyExcel.write(response.getOutputStream(), WxRepairExportDTO.class)
                .sheet("报修单数据")
                .doWrite(exportList);
    }
    
    
    
    
    @PostMapping("/wxRepair/cancelAssign")
    @RequiresPermissions(value = "wxRepair:assign")
    @SystemControllerLog(description = "取消维修人员分配")
    @ResponseBody
    public BasicDataResult cancelAssign(@RequestParam("repairId") String repairId) {
        try {
            WxRepair wxRepair = wxRepairService.cancelAssign(repairId);
            if (Objects.nonNull(wxRepair)) {
                return BasicDataResult.build(200, "取消分配成功", wxRepair);
            } else {
                return BasicDataResult.build(400, "报修单不存在", null);
            }
        } catch (RuntimeException e) {
            log.error("取消分配失败：", e);
            return BasicDataResult.build(400, e.getMessage(), null);
        } catch (Exception e) {
            log.error("系统异常：", e);
            return BasicDataResult.build(500, "系统异常", null);
        }
    }
    
    @PostMapping("/wxRepair/cancelRepair")
    @RequiresPermissions(value = "wxRepair:cancel")
    @SystemControllerLog(description = "取消维修")
    @ResponseBody
    public BasicDataResult cancelRepair(@RequestParam("repairId") String repairId) {
    	try {
    		WxRepair wxRepair = wxRepairService.cancelRepair(repairId);
    		if (Objects.nonNull(wxRepair)) {
    			return BasicDataResult.build(200, "取消成功", wxRepair);
    		} else {
    			return BasicDataResult.build(400, "报修单不存在", null);
    		}
    	} catch (RuntimeException e) {
    		log.error("取消分配失败：", e);
    		return BasicDataResult.build(400, e.getMessage(), null);
    	} catch (Exception e) {
    		log.error("系统异常：", e);
    		return BasicDataResult.build(500, "系统异常", null);
    	}
    }
    
    /**
     * 编辑报修人信息（整合版）
     */
    @PostMapping("/wxRepair/editWxReporter")
    @ResponseBody
    //@RequiresPermissions("wxRepair:editReporter")
    public BasicDataResult editWxReporter(WxReporter wxReporter) {
        try {
            // 参数校验
            if (wxReporter == null) {
                return BasicDataResult.build(500, "报修人信息不能为空", null);
            }

            // 区分新增（无ID）和编辑（有ID）
            if (wxReporter.getId() == null ) {
                // 新增时校验所有必填字段
                if (wxReporter.getUserName() == null || wxReporter.getUserName().trim().isEmpty()) {
                    return BasicDataResult.build(500, "报修人姓名不能为空", null);
                }
                if (wxReporter.getContactNumber() == null || wxReporter.getContactNumber().trim().isEmpty()) {
                    return BasicDataResult.build(500, "联系电话不能为空", null);
                }
                if (wxReporter.getSchoolName() == null || wxReporter.getSchoolName().trim().isEmpty()) {
                    return BasicDataResult.build(500, "报修学校不能为空", null);
                }
            } else {
                // 编辑时仅校验修改的字段（非空则校验格式）
                if (wxReporter.getContactNumber() != null && !wxReporter.getContactNumber().trim().isEmpty()) {
                    // 可选：校验手机号格式
                    if (!wxReporter.getContactNumber().matches("^1[3-9]\\d{9}$")) {
                        return BasicDataResult.build(500, "联系电话格式不正确", null);
                    }
                }
                // 其他字段编辑时仅判空（如果传了值则不能为空）
                if (wxReporter.getUserName() != null && wxReporter.getUserName().trim().isEmpty()) {
                    return BasicDataResult.build(500, "报修人姓名不能为空", null);
                }
                if (wxReporter.getSchoolName() != null && wxReporter.getSchoolName().trim().isEmpty()) {
                    return BasicDataResult.build(500, "报修学校不能为空", null);
                }
            }

            // 调用服务层方法
            WxReporter saveOrUpdate = wxReporterService.saveOrUpdate(wxReporter);
            if (saveOrUpdate != null) {
                // 修复：返回新增的ID，供前端同步
                return BasicDataResult.build(200, "编辑报修人信息成功", saveOrUpdate);
            } else {
                return BasicDataResult.build(500, "编辑报修人信息失败", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return BasicDataResult.build(500, "编辑报修人信息异常：" + e.getMessage(), null);
        }
    }

    
    @PostMapping("/wxRepair/cancelRepairToComplete")
    @RequiresPermissions(value = "wxRepair:cancel")
    @SystemControllerLog(description = "维修直接完成")
    @ResponseBody
    public BasicDataResult cancelRepairToComplete(@RequestParam("repairId") String repairId,String workDept) {
    	try {
    		WxRepair wxRepair = wxRepairService.cancelRepairToComplete(repairId,workDept);
    		if (Objects.nonNull(wxRepair)) {
    			return BasicDataResult.build(200, "维修状态修改成功", wxRepair);
    		} else {
    			return BasicDataResult.build(400, "报修单不存在", null);
    		}
    	} catch (RuntimeException e) {
    		log.error("取消分配失败：", e);
    		return BasicDataResult.build(400, e.getMessage(), null);
    	} catch (Exception e) {
    		log.error("系统异常：", e);
    		return BasicDataResult.build(500, "系统异常", null);
    	}
    }
    
    

}