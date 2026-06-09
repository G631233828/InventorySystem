package zhongchiedu.wechat.controller.wxrepair;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

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
import zhongchiedu.common.utils.BaiduOcrUtil;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.OcrUtil;
import zhongchiedu.common.utils.enums.PersonJoinAuditStatusEnum;
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

//	@PostMapping("/completeRepair")
//	@ResponseBody
//	public BasicDataResult completeRepair(@RequestParam("repairId") String repairId,
//	        @RequestParam("openId") String openId,
//	        @RequestParam(value = "repairPhotos", required = false) MultipartFile[] repairPhotos,
//	        @RequestParam(value = "triplicatePhotos", required = false) MultipartFile[] triplicatePhotos,
//	        @RequestParam("repairContent") String repairContent, HttpSession session) {
//
//	    try {
//	        if (Common.isEmpty(openId)) {
//	            return BasicDataResult.build(400, "该报修单状态异常，请返回维修列表页面重新进入", null);
//	        }
//	        // 1. 参数校验（避免空指针或无效ID）
//	        if (Common.isEmpty(repairId)) {
//	            return BasicDataResult.build(400, "报修单ID无效", null);
//	        }
//	        // 2. 调用业务层执行分配逻辑（核心业务，需你自行实现Service层）
//	        WxRepair wxRepair = this.wxRepairService.findOneById(repairId, WxRepair.class);
//
//	        String workerOpenId = wxRepair.getWorker().getOpenId();
//	        if (!openId.equals(workerOpenId)) {
//	            return BasicDataResult.build(400, "该报修单状态异常请联系管理人员！", null);
//	        }
//
//	        // 获取保修单的状态 状态必须要为3
//	        RepairStatus status = RepairStatus.fromCode(wxRepair.getStatus());
//	        if (status != RepairStatus.PROCESSING)
//	            return BasicDataResult.build(400, "该报修单状态异常请联系管理人员！", null);
//
//	        wxRepair.setRepairContent(repairContent);
//	        // 执行维修完成逻辑
//	        //boolean res = this.wxRepairService.completeRepair(wxRepair, repairPhotos, imgPath, dir);
//	        boolean res = this.wxRepairService.completeRepairWithTriplicate(wxRepair, repairPhotos, triplicatePhotos, imgPath, dir);
//	        // 3. 根据业务结果返回对应信息
//	        if (res) {
//	            // 构造推送消息的参数
//	            Map<String, String> map = new HashMap<>();
//	            map.put("thing12", Common.getOrDefault(wxRepair.getWxReporter().getSchoolName(), "")
//	                    + Common.getOrDefault(wxRepair.getWxReporter().getUserName(), "老师"));
//	            map.put("character_string11", Common.getOrDefault(wxRepair.getWorkOrderNumber(), "0000"));
//	            map.put("thing8", Common.getOrDefault(
//	                    wxRepair.getWorker().getName() + wxRepair.getWorker().getContactNumber(), "维修人员"));
//	            map.put("time2", Common.getDateYMDHM(wxRepair.getCreateTime()));
//	            map.put("time3", Common.getDateYMDHM(new Date()));
//
//	            // ========== 核心修改：同时获取调度人员和工程部人员 ==========
//	            // 1. 获取所有审核通过的调度人员
//	            List<WxBinding> dispatcherList = this.wxBindingService.findBindingsByPersonnelType(
//	                    PersonnelType.DISPATCHER, 
//	                    PersonJoinAuditStatusEnum.APPROVED);
//	            // 2. 获取所有审核通过的工程部人员
//	            List<WxBinding> engineeringDeptList = this.wxBindingService.findBindingsByPersonnelType(
//	                    PersonnelType.ENGINEERING_DEPARTMENT, 
//	                    PersonJoinAuditStatusEnum.APPROVED);
//	            
//	            // 3. 合并两个列表（避免重复推送逻辑）
//	            List<WxBinding> pushTargetList = new ArrayList<>();
//	            pushTargetList.addAll(dispatcherList);
//	            pushTargetList.addAll(engineeringDeptList);
//
//	            // 4. 统一执行消息推送
//	            if (!pushTargetList.isEmpty()) {
//	                pushTargetList.stream()
//	                        .filter(user -> Common.isNotEmpty(user.getOpenId())) // 过滤空OpenId
//	                        .forEach(user -> {
//	                            String sendWxMessage = this.wxMsgPush.sendWxMessage(
//	                                    templateId7, 
//	                                    user.getOpenId(),
//	                                    weburl + "/wechatrp/findWxRepair/" + repairId, 
//	                                    map);
//	                            log.info("维修单{}维修成功，向{}({})推送消息结果：{}", 
//	                                    repairId, user.getName(), user.getOpenId(), sendWxMessage);
//	                        });
//	            }
//
//	            return BasicDataResult.ok("维修成功");
//	        } else {
//	            return BasicDataResult.build(500, "提交失败，请联系管理员反馈问题", null);
//	        }
//
//	    } catch (Exception e) {
//	        // 4. 全局异常捕获（避免程序崩溃，返回友好提示）
//	        e.printStackTrace(); // 实际生产环境建议用日志框架记录（如Logback/SLF4J）
//	        return BasicDataResult.build(500, "系统异常，提交失败", null);
//	    }
//	}
	@PostMapping("/completeRepair")
	@ResponseBody
	public BasicDataResult completeRepair(@RequestParam("repairId") String repairId,
	        @RequestParam("openId") String openId,
	        @RequestParam(value = "repairPhotos", required = false) MultipartFile[] repairPhotos,
	        @RequestParam(value = "triplicatePhotos", required = false) MultipartFile[] triplicatePhotos,
	        @RequestParam("repairContent") String repairContent, HttpSession session) {

	    try {
	        if (Common.isEmpty(openId)) {
	            return BasicDataResult.build(400, "该报修单状态异常，请返回维修列表页面重新进入", null);
	        }
	        // 1. 参数校验（避免空指针或无效ID）
	        if (Common.isEmpty(repairId)) {
	            return BasicDataResult.build(400, "报修单ID无效", null);
	        }
	        
	        // ===================== 新增：报修单强制上传 + OCR校验 =====================
	        if (triplicatePhotos == null || triplicatePhotos.length == 0) {
	            return BasicDataResult.build(400, "请上传报修单照片", null);
	        }
	        
	        // OCR 识别图片文字
	        String ocrResult = BaiduOcrUtil.accurateOcr(triplicatePhotos[0]);
	        boolean isTriplicate = BaiduOcrUtil.isTriplicateReceipt(ocrResult);
	        
	        if (!isTriplicate) {
	            return BasicDataResult.build(400, "上传的图片不是有效报修单，请重新上传", null);
	        }
	        // ====================================================================
	        
	        // 2. 调用业务层执行分配逻辑（核心业务，需你自行实现Service层）
	        WxRepair wxRepair = this.wxRepairService.findOneById(repairId, WxRepair.class);

	        String workerOpenId = wxRepair.getWorker().getOpenId();
	        if (!openId.equals(workerOpenId)) {
	            return BasicDataResult.build(400, "该报修单状态异常请联系管理人员！", null);
	        }

	        // 获取保修单的状态 状态必须要为3
	        RepairStatus status = RepairStatus.fromCode(wxRepair.getStatus());
	        if (status != RepairStatus.PROCESSING)
	            return BasicDataResult.build(400, "该报修单状态异常请联系管理人员！", null);

	        wxRepair.setRepairContent(repairContent);
	        // 执行维修完成逻辑
	        //boolean res = this.wxRepairService.completeRepair(wxRepair, repairPhotos, imgPath, dir);
	        boolean res = this.wxRepairService.completeRepairWithTriplicate(wxRepair, repairPhotos, triplicatePhotos, imgPath, dir);
	        // 3. 根据业务结果返回对应信息
	        if (res) {
	            // 构造推送消息的参数
	            Map<String, String> map = new HashMap<>();
	            map.put("thing12", Common.getOrDefault(wxRepair.getWxReporter().getSchoolName(), "")
	                    + Common.getOrDefault(wxRepair.getWxReporter().getUserName(), "老师"));
	            map.put("character_string11", Common.getOrDefault(wxRepair.getWorkOrderNumber(), "0000"));
	            map.put("thing8", Common.getOrDefault(
	                    wxRepair.getWorker().getName() + wxRepair.getWorker().getContactNumber(), "维修人员"));
	            map.put("time2", Common.getDateYMDHM(wxRepair.getCreateTime()));
	            map.put("time3", Common.getDateYMDHM(new Date()));

	            // ========== 核心修改：同时获取调度人员和工程部人员 ==========
	            // 1. 获取所有审核通过的调度人员
	            List<WxBinding> dispatcherList = this.wxBindingService.findBindingsByPersonnelType(
	                    PersonnelType.DISPATCHER, 
	                    PersonJoinAuditStatusEnum.APPROVED);
	            // 2. 获取所有审核通过的工程部人员
	            List<WxBinding> engineeringDeptList = this.wxBindingService.findBindingsByPersonnelType(
	                    PersonnelType.ENGINEERING_DEPARTMENT, 
	                    PersonJoinAuditStatusEnum.APPROVED);
	            
	            // 3. 合并两个列表（避免重复推送逻辑）
	            List<WxBinding> pushTargetList = new ArrayList<>();
	            pushTargetList.addAll(dispatcherList);
	            pushTargetList.addAll(engineeringDeptList);

	            // 4. 统一执行消息推送
	            if (!pushTargetList.isEmpty()) {
	                pushTargetList.stream()
	                        .filter(user -> Common.isNotEmpty(user.getOpenId())) // 过滤空OpenId
	                        .forEach(user -> {
	                            String sendWxMessage = this.wxMsgPush.sendWxMessage(
	                                    templateId7, 
	                                    user.getOpenId(),
	                                    weburl + "/wechatrp/findWxRepair/" + repairId, 
	                                    map);
	                            log.info("维修单{}维修成功，向{}({})推送消息结果：{}", 
	                                    repairId, user.getName(), user.getOpenId(), sendWxMessage);
	                        });
	            }

	            return BasicDataResult.ok("维修成功");
	        } else {
	            return BasicDataResult.build(500, "提交失败，请联系管理员反馈问题", null);
	        }

	    } catch (Exception e) {
	        // 4. 全局异常捕获（避免程序崩溃，返回友好提示）
	        e.printStackTrace(); // 实际生产环境建议用日志框架记录（如Logback/SLF4J）
	        return BasicDataResult.build(500, "系统异常，提交失败", null);
	    }
	}
	
	
	
	
	
	
	@PostMapping("/checkTriplicateOcr")
	@ResponseBody
	public BasicDataResult checkTriplicateOcr(@RequestParam("triplicatePhoto") MultipartFile triplicatePhoto) {
	    try {
	        if (triplicatePhoto == null || triplicatePhoto.isEmpty()) {
	            return BasicDataResult.build(400, "请上传报修单图片", null);
	        }

	        // OCR 识别
	        String ocrResult = BaiduOcrUtil.accurateOcr(triplicatePhoto);
	        boolean isTriplicate = BaiduOcrUtil.isTriplicateReceipt(ocrResult);

	        if (isTriplicate) {
	            return BasicDataResult.ok("校验通过，是有效报修单");
	        } else {
	            return BasicDataResult.build(400, "上传的图片不是有效报修单，请重新上传", null);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        return BasicDataResult.build(500, "OCR 识别异常，请重试", null);
	    }
	}
	
	
	
	
	
	
	
	
	

}