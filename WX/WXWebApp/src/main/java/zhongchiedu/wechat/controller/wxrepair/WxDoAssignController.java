package zhongchiedu.wechat.controller.wxrepair;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.enums.PersonJoinAuditStatusEnum;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.wx.template.WxMsgPush;

/**
 * 报修单相关页面跳转Controller
 */
@Slf4j
@Controller
@RequestMapping("/wechatrp") // 与微信推送链接中的路径保持一致
public class WxDoAssignController {

	// 注入报修单服务（用于查询报修单数据）
	@Autowired
	private WxRepairService wxRepairService;

	@Autowired
	private WxBindingService wxBindingService;

	@Value("${templateId5}")
	private String templateId5; // 维修订单模版

	@Value("${qrcode.weburl}")
	private String weburl;

	@Autowired
	private WxMsgPush wxMsgPush;

	/**
	 * 调度人员任务分配
	 * 
	 * @param repairId
	 * @param id
	 * @return
	 */
	/**
	 * 调度人员任务分配
	 * 
	 * @param repairId 报修单ID
	 * @param id 施工队人员ID
	 * @param projectId 项目ID
	 * @param openId 操作用户OpenId
	 * @return BasicDataResult
	 */
	@PostMapping("/doAssign")
	@SystemControllerLog(description = "调度人员任务分配") // 核心：添加AOP切面所需注解
	@ResponseBody
	public BasicDataResult doAssign(@RequestParam("repairId") String repairId,
	        @RequestParam("id") String id,
	        @RequestParam("projectId") String projectId,
	        @RequestParam("openId") String openId) {

	    BasicDataResult result; // 声明返回结果对象
	    try {
	        // 1. 参数校验（避免空指针或无效ID）
	        if (Common.isEmpty(repairId)) { // 统一使用Common工具类判断空值，更规范
	            String errorMsg = "报修单ID无效，不能为空";
	            log.warn(errorMsg); // 记录参数错误日志
	            throw new IllegalArgumentException(errorMsg); // 主动抛出参数异常，让AOP捕获
	        }
	        if (Common.isEmpty(id)) {
	            String errorMsg = "施工队人员ID无效，不能为空";
	            log.warn(errorMsg);
	            throw new IllegalArgumentException(errorMsg);
	        }
	        if (Common.isEmpty(openId)) {
	            String errorMsg = "页面访问异常，未获取到OpenId";
	            log.warn(errorMsg);
	            throw new IllegalArgumentException(errorMsg);
	        }

	        // 查询操作用户的微信绑定信息
	        WxBinding findWxBindingByOpenId = this.wxBindingService.findWxBindingByOpenId(openId);
	        // 权限校验：必须是审核通过的调度人员才能操作
	        if (Common.isEmpty(findWxBindingByOpenId) ||
	                !findWxBindingByOpenId.getAuditStatus().equals(PersonJoinAuditStatusEnum.APPROVED.getCode()) ||
	                !findWxBindingByOpenId.getPersonnelType().equals(PersonnelType.DISPATCHER.getCode())) {
	            String errorMsg = "非调度人员访问或人员审核未通过，禁止分配任务";
	            log.warn(errorMsg); // 替换System.out为日志输出，更规范
	            throw new RuntimeException(errorMsg); // 主动抛出业务异常，让AOP捕获
	        }

	        // 2. 调用业务层执行分配逻辑
	        WxRepair wxRepair = wxRepairService.assignWorkerToRepair(repairId, id, projectId);

	        // 3. 根据业务结果处理
	        if (wxRepair != null) {
	            // 分配成功，执行推送消息
	            Map<String, String> map = new HashMap<>();

	            // 防护：避免wxReporter为空导致空指针
	            WxReporter reporter = wxRepair.getWxReporter();
	            if (reporter == null) {
	                reporter = new WxReporter();
	            }

	            // 1. thing4：学校+校区（避免null拼接）
	            String schoolName = Common.isNotEmpty(reporter.getSchoolName()) ? reporter.getSchoolName() : "未知学校";
	            String campus = Common.isNotEmpty(reporter.getCampus()) ? reporter.getCampus() : "未知校区";
	            map.put("thing4", schoolName + "校区：" + campus);

	            // 2. thing5：报修人姓名+联系电话
	            String userName = Common.isNotEmpty(reporter.getUserName()) ? reporter.getUserName() : "未知报修人";
	            String contactNumber = Common.isNotEmpty(reporter.getContactNumber()) ? reporter.getContactNumber() : "";
	            map.put("thing5", userName + contactNumber);

	            // 3. time2：期望时间
	            String expectedTime = (wxRepair.getExpectedVisitTime() != null)
	                    ? Common.getDateYMDHM(wxRepair.getExpectedVisitTime())
	                    : "未知期望时间";
	            map.put("time2", expectedTime);

	            // 4. thing16：紧急程度
	            String urgencyLevel = Common.isNotEmpty(wxRepair.getUrgencyLevel()) ? wxRepair.getUrgencyLevel() : "普通";
	            map.put("thing16", urgencyLevel);

	            // 5. thing11：故障描述
	            String faultInfo = Common.isNotEmpty(wxRepair.getFaultInformation()) ? wxRepair.getFaultInformation()
	                    : "无详细故障描述";
	            map.put("thing11", faultInfo);

	            // 防护：避免worker为空导致推送消息空指针
	            if (wxRepair.getWorker() == null) {
	                String errorMsg = "分配的维修人员信息为空，无法推送消息";
	                log.error(errorMsg);
	                throw new RuntimeException(errorMsg);
	            }

	            // 执行消息推送
	            String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId5, wxRepair.getWorker().getOpenId(),
	                    weburl + "/wechatrp/findWxRepairByWorker/" + wxRepair.getId(), map);
	            log.info("调度人员分配任务成功，向维修人员[{}]推送消息结果：{}", wxRepair.getWorker().getOpenId(), sendWxMessage);
	            
	            
	            //记录assignTime 分配任务时间
	             wxRepair.setAssignTime(Common.getDateYMDHM(new Date()));
	             this.wxRepairService.save(wxRepair);
	                
	            
	            
	            
	            result = BasicDataResult.ok("分配成功"); // 构建成功结果
	        } else {
	            String errorMsg = "分配失败，请检查报修单状态或施工队人员信息";
	            log.error(errorMsg);
	            throw new RuntimeException(errorMsg); // 业务失败主动抛异常，让AOP记录
	        }

	    } catch (Exception e) {
	        // 4. 全局异常处理：记录日志 + 重新抛出异常（让AOP捕获）
	        log.error("调度人员任务分配异常：", e); // 记录完整异常栈
	        
	        // 区分异常类型，返回友好提示
	        String errorMsg = "系统异常，分配失败";
	        if (e instanceof IllegalArgumentException) {
	            errorMsg = "参数错误：" + e.getMessage(); // 参数异常提示
	        } else if (e instanceof RuntimeException) {
	            errorMsg = e.getMessage(); // 业务异常提示
	        }
	        
	        // 构建错误返回结果
	        result = BasicDataResult.build(500, errorMsg, null);
	        
	        // 核心：包装原异常重新抛出，确保AOP的@AfterThrowing能捕获
	        throw new RuntimeException("调度人员任务分配异常：" + e.getMessage(), e);
	    }
	    return result;
	}

}