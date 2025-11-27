package zhongchiedu.wechat.controller.wxrepair;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.common.utils.enums.RepairStatus;
import zhongchiedu.general.pojo.MultiMedia;
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
public class WxCompleteRepairController {

	// 注入报修单服务（用于查询报修单数据）
	@Autowired
	private WxRepairService wxRepairService;

	@Autowired
	private WxBindingService wxBindingService;

	@Value("${templateId7}")
	private String templateId7; // 维修订单模版

	@Value("${qrcode.weburl}")
	private String weburl;

	@Autowired
	private WxMsgPush wxMsgPush;
	
	@Value("${upload-imgpath}")
	private String imgPath;
	
	@Value("${upload-dir}")
	private String dir;

	/**
	 * 维修人员完成维修
	 * 
	 * @param repairId
	 * @param id
	 * @return
	 */	

	@PostMapping("/completeRepair")
	@ResponseBody
	public BasicDataResult completeRepair(@RequestParam("repairId") String repairId,
			@RequestParam(value = "repairPhotos", required = false) MultipartFile[] repairPhotos,
			@RequestParam("repairContent") String repairContent) {

		try {
			// 1. 参数校验（避免空指针或无效ID）
			if (repairId == null) {
				return BasicDataResult.build(400, "报修单ID无效", null);
			}
			// 2. 调用业务层执行分配逻辑（核心业务，需你自行实现Service层）
			WxRepair wxRepair = this.wxRepairService.findOneById(repairId, WxRepair.class);
			//获取保修单的状态  状态必须要为3 
			RepairStatus status = RepairStatus.fromCode(wxRepair.getStatus());
			if(status != RepairStatus.PROCESSING)
				return BasicDataResult.build(400, "该报修单状态不是处理中！", null);
			wxRepair.setRepairContent(repairContent);
			//执行维修完成逻辑
			boolean res =  this.wxRepairService.completeRepair(wxRepair,repairPhotos,imgPath,dir);
			
			// 3. 根据业务结果返回对应信息
			if (res) {
//				客户名称				{{thing12.DATA}}
//				维修单号				{{character_string11.DATA}}
//				处理人				{{thing8.DATA}}
//				申请时间				{{time2.DATA}}
//				完成时间				{{time3.DATA}}
				// 分配成功 執行推送消息
				List<WxBinding> findBindingsByPersonnelType = this.wxBindingService.findBindingsByPersonnelType(PersonnelType.DISPATCHER);//拿到所有调度人员
				if(findBindingsByPersonnelType.size()>0) {
					Map<String, String> map = new HashMap<>();
					map.put("thing12",Common.getOrDefault(wxRepair.getWxReporter().getSchoolName(), "")+ Common.getOrDefault(wxRepair.getWxReporter().getUserName(), "老师"));
					map.put("character_string11",  Common.getOrDefault(wxRepair.getWorkOrderNumber(), "0000"));
					map.put("thing8",  Common.getOrDefault(wxRepair.getWorker().getName()+wxRepair.getWorker().getContactNumber(), "维修人员"));
					map.put("time2",  Common.getDateYMDHM(wxRepair.getCreateTime()));
					map.put("time3",Common.getDateYMDHM(new Date()));
					//执行推送 给商务
					findBindingsByPersonnelType.stream().filter(user -> Common.isNotEmpty(user.getOpenId())).forEach(user -> {
						String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId7, user.getOpenId(),
								weburl + "/wechatrp/findWxRepair/"+repairId , map);
						log.info("维修单{}维修成功，消息推送成功：{}",repairId , sendWxMessage);
					});
				}
				return BasicDataResult.ok("维修成功"); // 状态200，消息"分配成功"，无额外数据
			} else {
				return BasicDataResult.build(500, "提交失败，请联系管理员反馈问题", null);
			}

		} catch (Exception e) {
			// 4. 全局异常捕获（避免程序崩溃，返回友好提示）
			e.printStackTrace(); // 实际生产环境建议用日志框架记录（如Logback/SLF4J）
			return BasicDataResult.build(500, "系统异常，提交失败", null);
		}
	}
	
	
	
	
	
	
	
	

}