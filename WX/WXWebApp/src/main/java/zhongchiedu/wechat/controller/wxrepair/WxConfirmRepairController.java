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
public class WxConfirmRepairController {

	// 注入报修单服务（用于查询报修单数据）
	@Autowired
	private WxRepairService wxRepairService;

	@Autowired
	private WxBindingService wxBindingService;

	@Value("${templateId6}")
	private String templateId6; // 维修订单模版

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
	@PostMapping("/confirmRepair")
	@ResponseBody
	public BasicDataResult confirmRepair(@RequestParam("repairId") String repairId) {

		try {
			// 1. 参数校验（避免空指针或无效ID）
			if (repairId == null) {
				return BasicDataResult.build(400, "报修单ID无效", null);
			}
			

			// 2. 调用业务层执行分配逻辑（核心业务，需你自行实现Service层）
			WxRepair wxRepair = wxRepairService.confirmRepair(repairId);

			// 3. 根据业务结果返回对应信息
			if (wxRepair != null) {
				// 分配成功 執行推送消息
				List<WxBinding> findBindingsByPersonnelType = this.wxBindingService.findBindingsByPersonnelType(PersonnelType.DISPATCHER);//拿到所有调度人员
				if(findBindingsByPersonnelType.size()>0) {
					Map<String, String> map = new HashMap<>();
					map.put("character_string11", Common.getOrDefault(wxRepair.getWorkOrderNumber(), "未知工单号"));
					map.put("time13", Common.getDateYMDHM(new Date()));
					map.put("thing4", wxRepair.getWorker().getName());
					map.put("phone_number5", wxRepair.getWorker().getContactNumber());
					map.put("thing6",wxRepair.getWxReporter().getSchoolName()+"("+wxRepair.getWxReporter().getCampus()+")");
					//执行推送
					findBindingsByPersonnelType.stream().filter(user -> Common.isNotEmpty(user.getOpenId())).forEach(user -> {
						String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId6, user.getOpenId(),
								weburl + "/wechatrp/findWxRepairlist" , map);
						log.info("报修单{}接单成功，消息推送成功：{}",repairId , sendWxMessage);
					});
				}
				return BasicDataResult.ok("接单成功"); // 状态200，消息"分配成功"，无额外数据
			} else {
				return BasicDataResult.build(500, "接单失败，请联系管理员反馈问题", null);
			}

		} catch (Exception e) {
			// 4. 全局异常捕获（避免程序崩溃，返回友好提示）
			e.printStackTrace(); // 实际生产环境建议用日志框架记录（如Logback/SLF4J）
			return BasicDataResult.build(500, "系统异常，分配失败", null);
		}
	}
	
	
	
	
	
	
	
	

}