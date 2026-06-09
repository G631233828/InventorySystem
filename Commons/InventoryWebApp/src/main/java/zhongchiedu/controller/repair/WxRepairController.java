package zhongchiedu.controller.repair;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alibaba.excel.EasyExcel;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.common.utils.enums.PersonJoinAuditStatusEnum;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.Dto.WxRepairExportDTO;
import zhongchiedu.inventory.Dto.WxRepairImportDTO;
import zhongchiedu.inventory.pojo.AfterSalesProjects;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.AfterSalesProjectsService;
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
    
	@Autowired
	private AfterSalesProjectsService afterSalesProjectsService;
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
	    
	    
        List<AfterSalesProjects> findAllinServiceProj = this.afterSalesProjectsService.findAllinServiceProj();
        model.addAttribute("projs", findAllinServiceProj);
	    
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
                                        @RequestParam("workerId") String workerId,
                                        @RequestParam("projectId") String projectId) {
        // 声明返回结果对象
        BasicDataResult result;
        try {
            // 1. 核心业务逻辑：分配维修人员
            WxRepair wxRepair = wxRepairService.assignWorkerToRepair(repairId, workerId, projectId);
            
            if (Objects.nonNull(wxRepair)) {
                // 分配成功 执行推送消息
                Map<String, String> map = new HashMap<>();

                // 先处理 wxReporter 空值（核心：避免 wxReporter 为 null 导致后续调用抛空指针）
                WxReporter reporter = wxRepair.getWxReporter();
                if (reporter == null) {
                    reporter = new WxReporter(); // 若为 null，创建空对象避免后续频繁判断
                }

                // 1. thing4：学校+校区（分别校验空值，避免拼接出"null校区：null"）
                String schoolName = Common.isNotEmpty(reporter.getSchoolName()) ? reporter.getSchoolName() : "未知学校";
//                String campus = Common.isNotEmpty(reporter.getCampus()) ? reporter.getCampus() : "未知校区";
                map.put("thing4", schoolName );

                // 2. thing5：报修人姓名（默认"未知报修人"）
                String userName = Common.isNotEmpty(reporter.getUserName()) ? reporter.getUserName() : "未知报修人";
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

                // 校验worker是否为空，避免空指针
                if (wxRepair.getWorker() == null) {
                    throw new RuntimeException("分配的维修人员信息为空，无法推送消息");
                }
                // 执行推送
                String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId5, wxRepair.getWorker().getOpenId(),
                        weburl + "/wechatrp/findWxRepairByWorker/" + wxRepair.getId(), map);
                log.info("用户[{}]提交报修单成功，消息推送成功：{}", wxRepair.getWorker().getOpenId(), sendWxMessage);
               
                //记录assignTime 分配任务时间
                wxRepair.setAssignTime(Common.getDateYMDHM(new Date()));
                this.wxRepairService.save(wxRepair);
                
                
                // 构建成功结果
                result = BasicDataResult.build(200, "维修人员分配成功", wxRepair);
            } else {
                // 业务异常：分配失败（主动抛出运行时异常，让AOP捕获）
                String errorMsg = "维修人员分配失败：未找到对应的报修单或维修人员";
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (Exception e) {
            // 2. 捕获所有异常，记录本地日志，并重新抛出让AOP切面捕获
            log.error("分配维修人员失败：", e);
            
            // 区分异常类型，返回友好提示
            String errorMsg = "系统异常";
            if (e instanceof IllegalArgumentException) {
                errorMsg = "参数错误：" + e.getMessage();
            } else if (e instanceof RuntimeException) {
                errorMsg = e.getMessage();
            }
            
            // 构建错误返回结果
            result = BasicDataResult.build(500, errorMsg, null);
            
            // 核心：将异常包装为RuntimeException重新抛出，确保AOP的@AfterThrowing能捕获到
            throw new RuntimeException("分配维修人员异常：" + e.getMessage(), e);
        }
        return result;
    }
    
    
    /**
     * 催单功能（向已分配的维修人员推送催单消息）
     */
    @PostMapping("/wxRepair/remindWorker")
    @RequiresPermissions(value = "wxRepair:assign")
    @SystemControllerLog(description = "报修单催单")
    @ResponseBody
    public BasicDataResult remindWorker(@RequestParam("repairId") String repairId) {
        try {
            // 1. 校验报修单是否存在
            WxRepair wxRepair = wxRepairService.findOneById(repairId,WxRepair.class);
            if (Objects.isNull(wxRepair)) {
                String errorMsg = "催单失败：未找到对应的报修单";
                log.error(errorMsg);
                return BasicDataResult.build(500, errorMsg, null);
            }

            // 2. 校验报修单状态
            Integer status = wxRepair.getStatus();
            if (!Objects.equals(status, 2) && !Objects.equals(status, 3)) {
                String errorMsg = "催单失败：仅已分配（状态2）/处理中（状态3）的报修单可催单";
                log.error(errorMsg);
                return BasicDataResult.build(500, errorMsg, null);
            }

            // 3. 校验维修人员（核心：Worker而非WxReporter）
            WxReporter worker = wxRepair.getWxReporter();
            if (Objects.isNull(worker) || Common.isEmpty(worker.getOpenId())) {
                String errorMsg = "催单失败：该报修单未分配维修人员，无法推送催单消息";
                log.error(errorMsg);
                return BasicDataResult.build(500, errorMsg, null);
            }

            // 4. 构建催单消息模板参数（确保所有必填字段非空）
            Map<String, String> map = new HashMap<>();
            WxReporter reporter = wxRepair.getWxReporter() == null ? new WxReporter() : wxRepair.getWxReporter();
            
            // 4.1 学校+校区（非空兜底）
            String schoolName = Common.isNotEmpty(reporter.getSchoolName()) ? reporter.getSchoolName() : "未知学校";
//            String campus = Common.isNotEmpty(reporter.getCampus()) ? reporter.getCampus() : "未知校区";
            map.put("thing4","催单通知：" +schoolName );

            String userName = Common.isNotEmpty(reporter.getUserName()) ? reporter.getUserName() : "未知报修人";
            map.put("thing5", userName);

            map.put("time2", Common.getDateYMDHM(new Date()));

            // 4.5 紧急程度（非空兜底）
            String urgencyLevel = Common.isNotEmpty(wxRepair.getUrgencyLevel()) ? wxRepair.getUrgencyLevel() : "普通";
            map.put("thing16", urgencyLevel);
            
            String faultInfo = Common.isNotEmpty(wxRepair.getFaultInformation()) ? wxRepair.getFaultInformation()
                    : "无详细故障描述";
            map.put("thing11", faultInfo);


            // 5. 推送催单消息（核心：精准捕获微信异常）
            try {
                // 前置校验：模板ID/OpenId非空
                if (Common.isEmpty(templateId5)) {
                    String errorMsg = "催单失败：微信模板ID未配置";
                    log.error(errorMsg);
                    return BasicDataResult.build(500, errorMsg, null);
                }
                if (Common.isEmpty(worker.getOpenId())) {
                    String errorMsg = "催单失败：维修人员OpenId为空";
                    log.error(errorMsg);
                    return BasicDataResult.build(500, errorMsg, null);
                }
                
                // 执行微信推送
                String sendWxMessage = this.wxMsgPush.sendWxMessage(
                    templateId5, 
                    worker.getOpenId(),
                    weburl + "/wechatrp/findWxRepairByWorker/" + wxRepair.getId(), 
                    map
                );
                log.info("报修单[{}]催单消息推送成功：{}", repairId, sendWxMessage);

                // 只有推送成功，才返回200
                return BasicDataResult.build(200, "催单消息已推送至维修人员", wxRepair);
                
            } catch (Exception msgE) {
                // 捕获其他推送异常
                String errorMsg = "催单失败：消息推送异常 - " + msgE.getMessage();
                log.error(errorMsg, msgE);
                return BasicDataResult.build(500, errorMsg, null);
            }

        } catch (Exception e) {
            // 兜底捕获所有异常
            String errorMsg = "催单失败：系统异常 - " + e.getMessage();
            log.error(errorMsg, e);
            return BasicDataResult.build(500, errorMsg, null);
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

        // 定义时间格式化器（避免重复创建）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
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
            dto.setFindReadTime(repair.getFindReadTime()!=null?repair.getFindReadTime():"未打开");
            
            // 格式化创建时间，并补全默认值
            String formattedCreateTime = "无";
            Date createTime = repair.getCreateTime();
            if (createTime != null) {
                formattedCreateTime = sdf.format(createTime);
                dto.setCreateTime(formattedCreateTime);
            } else {
                dto.setCreateTime(formattedCreateTime);
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
            dto.setUseTime(Common.calculateUseTime(createTime, repair.getCompleteTime(), sdf));
            
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
//    @PostMapping("/wxRepair/editWxReporter")
//    @ResponseBody
//    //@RequiresPermissions("wxRepair:editReporter")
//    public BasicDataResult editWxReporter(WxReporter wxReporter) {
//        try {
//            // 参数校验
//            if (wxReporter == null) {
//                return BasicDataResult.build(500, "报修人信息不能为空", null);
//            }
//
//            // 区分新增（无ID）和编辑（有ID）
//            if (wxReporter.getId() == null ) {
//                // 新增时校验所有必填字段
//                if (wxReporter.getUserName() == null || wxReporter.getUserName().trim().isEmpty()) {
//                    return BasicDataResult.build(500, "报修人姓名不能为空", null);
//                }
//                if (wxReporter.getContactNumber() == null || wxReporter.getContactNumber().trim().isEmpty()) {
//                    return BasicDataResult.build(500, "联系电话不能为空", null);
//                }
//                if (wxReporter.getSchoolName() == null || wxReporter.getSchoolName().trim().isEmpty()) {
//                    return BasicDataResult.build(500, "报修学校不能为空", null);
//                }
//            } else {
//                // 编辑时仅校验修改的字段（非空则校验格式）
//                if (wxReporter.getContactNumber() != null && !wxReporter.getContactNumber().trim().isEmpty()) {
//                    // 可选：校验手机号格式
//                    if (!wxReporter.getContactNumber().matches("^1[3-9]\\d{9}$")) {
//                        return BasicDataResult.build(500, "联系电话格式不正确", null);
//                    }
//                }
//                // 其他字段编辑时仅判空（如果传了值则不能为空）
//                if (wxReporter.getUserName() != null && wxReporter.getUserName().trim().isEmpty()) {
//                    return BasicDataResult.build(500, "报修人姓名不能为空", null);
//                }
//                if (wxReporter.getSchoolName() != null && wxReporter.getSchoolName().trim().isEmpty()) {
//                    return BasicDataResult.build(500, "报修学校不能为空", null);
//                }
//            }
//
//            // 调用服务层方法
//            WxReporter saveOrUpdate = wxReporterService.saveOrUpdate(wxReporter);
//            if (saveOrUpdate != null) {
//                // 修复：返回新增的ID，供前端同步
//                return BasicDataResult.build(200, "编辑报修人信息成功", saveOrUpdate);
//            } else {
//                return BasicDataResult.build(500, "编辑报修人信息失败", null);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return BasicDataResult.build(500, "编辑报修人信息异常：" + e.getMessage(), null);
//        }
//    }

    
    /**
     * 编辑WxRepair单个字段（双击单元格修改）
     */
    @PostMapping("/wxRepair/editWxRepairField")
//    @RequiresPermissions(value = "wxRepair:edit")
    @SystemControllerLog(description = "编辑报修单单个字段")
    @ResponseBody
    public BasicDataResult editWxRepairField(
            @RequestParam("repairId") String repairId,
            @RequestParam("field") String field,
            @RequestParam("value") String value) {
        try {
            // 1. 校验参数
            if (Common.isEmpty(repairId)) {
                return BasicDataResult.build(500, "报修单ID不能为空", null);
            }
            if (Common.isEmpty(field)) {
                return BasicDataResult.build(500, "修改字段不能为空", null);
            }
            if (Common.isEmpty(value)) {
                return BasicDataResult.build(500, "修改值不能为空", null);
            }

            // 2. 查询报修单
            WxRepair wxRepair = wxRepairService.findOneById(repairId, WxRepair.class);
            if (wxRepair == null) {
                return BasicDataResult.build(500, "报修单不存在", null);
            }

            // 3. 获取WxReporter（不存在则创建）
            WxReporter wxReporter = wxRepair.getWxReporter();
            if (wxReporter == null) {
                wxReporter = new WxReporter();
                wxRepair.setWxReporter(wxReporter);
            }

            // 4. 根据字段名设置值
            switch (field) {
                case "schoolName":
                    wxReporter.setSchoolName(value);
                    break;
                case "campus":
                    wxReporter.setCampus(value);
                    break;
                case "schoolAddress":
                    wxReporter.setSchoolAddress(value);
                    break;
                case "userName":
                    wxReporter.setUserName(value);
                    break;
                case "contactNumber":
                    wxReporter.setContactNumber(value);
                    // 可选：手机号格式校验
                    if (!value.matches("^1[3-9]\\d{9}$")) {
                        return BasicDataResult.build(500, "手机号格式不正确", null);
                    }
                    break;
                default:
                    return BasicDataResult.build(500, "不支持的修改字段：" + field, null);
            }

            // 5. 保存报修单（级联保存WxReporter）
            wxRepairService.save(wxRepair);
            
            return BasicDataResult.build(200, "修改成功", wxRepair);
            
        } catch (Exception e) {
            log.error("编辑报修单字段失败：", e);
            return BasicDataResult.build(500, "修改失败：" + e.getMessage(), null);
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
    @RequestMapping("/wxRepair/import")
    @ResponseBody
    // @RequiresPermissions("wxRepair:import")
    @SystemControllerLog(description = "导入报修单")
    public ModelAndView importExcel(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
        log.info("开始上传报修单Excel文件");
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/wxRepairs");
        String error = wxRepairService.upload(request, session);
        attr.addFlashAttribute("errorImport", error);
        return modelAndView;
    }
    
    /**
     * 下载报修单导入模板
     */
    @GetMapping("/wxRepair/downloadTemplate")
    @SystemControllerLog(description = "下载报修单导入模板")
//    @RequiresPermissions("wxRepair:import")
    public ModelAndView downloadTemplate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String storeName = "报修单导入模版.xlsx";
        String contentType = "application/octet-stream";
        String UPLOAD = "Templates/";
        FileOperateUtil.download(request, response, storeName, contentType, UPLOAD);
        return null;
    }

    /**
     * 获取导入进度
     */
    @GetMapping("/wxRepair/uploadprocess")
    @ResponseBody
    public Object uploadprocess(HttpServletRequest request) {
        return wxRepairService.findproInfo(request);
    }
    
    /**
     * 单独修改维修备注（双击备注模态框提交）
     */
    @PostMapping("/wxRepair/updateRepairRemark")
//    @RequiresPermissions(value = "wxRepair:edit")
    @SystemControllerLog(description = "修改维修备注")
    @ResponseBody
    public BasicDataResult updateRepairRemark(
            @RequestParam("repairId") String repairId,
            @RequestParam(value = "remark", required = false) String remark) {
        try {
            if (Common.isEmpty(repairId)) {
                return BasicDataResult.build(500, "报修单ID不能为空", null);
            }

            WxRepair repair = wxRepairService.findOneById(repairId, WxRepair.class);
            if (repair == null) {
                return BasicDataResult.build(500, "报修单不存在", null);
            }

            // 设置备注
            repair.setRemark(remark);
            wxRepairService.save(repair);

            return BasicDataResult.build(200, "备注保存成功", repair);

        } catch (Exception e) {
            log.error("修改维修备注失败：", e);
            return BasicDataResult.build(500, "保存失败：" + e.getMessage(), null);
        }
    }
    

}