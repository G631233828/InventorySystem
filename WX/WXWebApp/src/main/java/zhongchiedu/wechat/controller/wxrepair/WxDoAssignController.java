package zhongchiedu.wechat.controller.wxrepair;

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
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
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
	@PostMapping("/doAssign")
	@ResponseBody
	public BasicDataResult doAssign(@RequestParam("repairId") String repairId, @RequestParam("id") String id) {

		try {
			// 1. 参数校验（避免空指针或无效ID）
			if (repairId == null) {
				return BasicDataResult.build(400, "报修单ID无效", null);
			}
			if (id == null) {
				return BasicDataResult.build(400, "施工队人员ID无效", null);
			}

			// 2. 调用业务层执行分配逻辑（核心业务，需你自行实现Service层）
			WxRepair wxRepair = wxRepairService.assignWorkerToRepair(repairId, id);

			// 3. 根据业务结果返回对应信息
			if (wxRepair != null) {
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
				
				map.put("thing5", userName+contactNumber);

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
				return BasicDataResult.ok("分配成功"); // 状态200，消息"分配成功"，无额外数据
			} else {
				return BasicDataResult.build(500, "分配失败，请检查报修单状态或施工队人员信息", null);
			}

		} catch (Exception e) {
			// 4. 全局异常捕获（避免程序崩溃，返回友好提示）
			e.printStackTrace(); // 实际生产环境建议用日志框架记录（如Logback/SLF4J）
			return BasicDataResult.build(500, "系统异常，分配失败", null);
		}
	}

}