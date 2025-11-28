package zhongchiedu.wechat.controller.wxrepair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.gargoylesoftware.htmlunit.javascript.host.Console;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
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
import zhongchiedu.inventory.service.WxReporterService;
import zhongchiedu.wx.config.WxMpProperties;
import zhongchiedu.wx.template.WxMsgPush;

@Controller
@RequestMapping("/wechatrp")
@CrossOrigin
@Slf4j
public class WxRepairController {

	// 注入业务层（根据实际情况替换为你的Service）
	@Autowired
	private WxRepairService wxRepairService;
	
	@Autowired
	private WxReporterService wxReporterService;
	
	@Autowired
	private  WxMsgPush wxMsgPush;
	
	
	@Value("${templateId4}")
	private String templateId4; //维修订单模版

	@Value("${upload-imgpath}")
	private String imgPath;
	
	@Value("${upload-dir}")
	private String dir;

	@Autowired
	private WxMpService wxMpService;
	@Autowired
	private WxMpProperties wxMpProperties;
	
	@Value("${qrcode.weburl}")
	private String weburl;
	
	@Autowired
	private WxBindingService wxBindingService;

	
	/**
	 *  教师访问的 repairlist  查看所有报修信息
	 * @param request
	 * @param model
	 * @param session
	 * @param status
	 * @param dateRange
	 * @param search
	 * @return
	 */
	@GetMapping(value = "/repairlist")
	public String repairlist(HttpServletRequest request, Model model, HttpSession session,
			   @RequestParam(required = false) String status,
	            @RequestParam(required = false) String dateRange,
	            @RequestParam(required = false) String search) {
	    try {
	        // 1. 先从 session 中查找是否已经有 openId
	        String openId = (String) session.getAttribute("openId");

	        if (openId == null) {
	            // 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
	            String code = request.getParameter("code");

	            if (code == null) {
	                // 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
	                String redirect_uri = weburl + "/wechatrp/repairlist";
	                return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + 
	                       "appid=" + wxMpProperties.getConfigs().get(0).getAppId() + 
	                       "&redirect_uri=" + URLEncoder.encode(redirect_uri, "UTF-8") + 
	                       "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
	            }
	            
	            // 使用 code 调用微信接口获取 openId
	            WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
	            WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
	            openId = accessToken.getOpenId();

	            // 3. 将获取到的 openId 存入 session，以便后续请求使用
	            session.setAttribute("openId", openId);
	            
	            // 可以顺便把用户信息也存入 session
	            WxMpUser userInfo = wxMpService.getUserService().userInfo(openId);
	            session.setAttribute("userInfo", userInfo);
	        } else {
	            // 4. 如果 session 中已经有 openId，说明是重复请求（如刷新），直接从 session 中获取
	            System.out.println("openId 已存在于 session 中，直接使用: " + openId);
	        }

	        // 此时 openId 一定是有效的，可以安全地使用它来查询数据
	        List<WxRepair> repairList = this.wxRepairService.findWxRepairByOpenId(openId);
	        model.addAttribute("repairList", repairList);
	        model.addAttribute("openId", openId);
	        model.addAttribute("userInfo", session.getAttribute("userInfo"));

	    } catch (WxErrorException | UnsupportedEncodingException e) {
	        // 如果是因为 code 无效（如刷新导致），则清除 session 并重定向
	        if (((WxErrorException) e).getError().getErrorCode() == 40163) {
	            session.invalidate(); // 清除无效的 session
	            String redirect_uri = weburl + "/wechatrp/repairlist";
	            return "redirect:" + redirect_uri; // 重定向到当前页面，会触发新的授权流程
	        }
	        e.printStackTrace();
	        // 其他错误处理...
	        return "error"; 
	    }
	    
	    return "school/repairlist";
	}

//	@GetMapping(value = "/repair")
//	public String torepair(HttpServletRequest request,Model model) {
//		try {
//			String code = request.getParameter("code");
//			// 判断code是否为空，如果为空的话需要通过微信进行重定向
//			if (Common.isEmpty(code)) {
//				String redirect_uri = wxMpProperties.getServerUrl() + "/WXWebApp/wechatrp/repair";
//				return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
//						+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri=" + redirect_uri
//						+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
//			}
//
//			WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
//			WxOAuth2AccessToken accessToken;
//			accessToken = oAuth2Service.getAccessToken(code);
//
////		WxMpOAuth2AccessToken oauth2getAccessToken = wxMpService.oauth2getAccessToken(code);
//			String openId = accessToken.getOpenId();
//
//			// 1. 校验OpenID是否存在
//			if (openId == null || openId.trim().isEmpty()) {
//				log.warn("提交报修失败：未获取到用户OpenID");
//				return "school/repair";
//			}
//			model.addAttribute("openId", openId);
//			
//
//		} catch (WxErrorException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return "school/repair";
//	}

	@GetMapping(value = "/repair")
	public String torepair(HttpServletRequest request, Model model) {
	    // 1. 先从 Session 中获取 openid，看看用户是否已经登录
	    String openId = (String) request.getSession().getAttribute("openId");
	    
	    WxReporter wxReporter = this.wxReporterService.findWxReporterByOpenId(openId);
        if(wxReporter!=null) {
        	  model.addAttribute("wxReporter", wxReporter);
        }
        
	    // 如果 Session 中已经有 openid，说明用户已经授权过了，直接跳转到页面即可
	    if (openId != null && !openId.trim().isEmpty()) {
	        model.addAttribute("openId", openId);
	        return "school/repair";
	    }

	    try {
	        String code = request.getParameter("code");

	        // 2. 如果 code 为空，说明是首次访问，需要重定向到微信授权页
	        if (code == null || code.trim().isEmpty()) {
	            String redirect_uri = weburl + "/wechatrp/repair";
	            // 注意：这里最好对 redirect_uri 进行 URLEncode
	            String encodedRedirectUri = URLEncoder.encode(redirect_uri, "UTF-8");
	            String authUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?"
	                    + "appid=" + wxMpProperties.getConfigs().get(0).getAppId()
	                    + "&redirect_uri=" + encodedRedirectUri
	                    + "&response_type=code"
	                    + "&scope=snsapi_userinfo"
	                    + "&state=STATE#wechat_redirect";
	            return "redirect:" + authUrl;
	        }

	        // 3. 如果 code 不为空，说明是微信授权后跳转回来的回调请求
	        WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
	        WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);

	        openId = accessToken.getOpenId();
	        //通过openId 去wxReporter 中获取报修人信息
	        if (openId == null || openId.trim().isEmpty()) {
	            log.warn("提交报修失败：未获取到用户OpenID");
	            // 获取失败，可以跳转到一个错误提示页
	            return "school/repair_error"; 
	        }

	        // 4. 关键步骤：将获取到的 openid 存入 Session
	        request.getSession().setAttribute("openId", openId);
	        model.addAttribute("openId", openId);

	    } catch (WxErrorException e) {
	        // 5. 异常处理：如果获取 access_token 失败（比如 code 已使用、过期等）
	        log.error("获取微信 access_token 失败: {}", e.getMessage());
	        e.printStackTrace();

	        // 可以考虑清除 session 并重定向到首页，让用户重新发起授权
	        request.getSession().removeAttribute("openId");
	        return "redirect:/WXWebApp/wechatrp/repair"; 
	    } catch (UnsupportedEncodingException e) {
	        log.error("URL编码失败: {}", e.getMessage());
	        e.printStackTrace();
	    }

	    return "school/repair";
	}
	/**
	 * 提交报修单
	 *
	 * @param openid                微信用户OpenID，由微信服务器通过Header传入
	 * @param schoolName            学校名称
	 * @param schoolAddress         学校地址
	 * @param campus                校区
	 * @param userName              联系人姓名
	 * @param contactNumber         联系电话
	 * @param reportClassroomRepair 报修教室
	 * @param equipmentRepair       报修设备
	 * @param faultInformation      故障信息
	 * @param urgencyLevel          紧急程度
	 * @param expectedVisitTime     期望上门时间
	 * @param photos                故障照片
	 * @return BasicDataResult
	 */
	@PostMapping("/wxRepair")
	@ResponseBody
	public BasicDataResult submitRepair(@RequestParam("schoolName") String schoolName,
			@RequestParam("openId") String openId,
			@RequestParam("schoolAddress") String schoolAddress, @RequestParam("campus") String campus,
			@RequestParam("userName") String userName, @RequestParam("contactNumber") String contactNumber,
			@RequestParam("reportClassroomRepair") String reportClassroomRepair,
			@RequestParam("equipmentRepair") String equipmentRepair,
			@RequestParam("faultInformation") String faultInformation,
			@RequestParam("urgencyLevel") String urgencyLevel,
			@RequestParam("expectedVisitTime") String expectedVisitTime,
			@RequestParam(value = "photos", required = false) MultipartFile[] photos, HttpServletRequest request) {
		try {
			 // 1. 处理报修人信息：保存或更新（通过openId判断是否存在）
            WxReporter reporter = new WxReporter();
            reporter.setOpenId(openId);
            reporter.setSchoolName(schoolName);
            reporter.setSchoolAddress(schoolAddress);
            reporter.setCampus(campus);
            reporter.setUserName(userName);
            reporter.setContactNumber(contactNumber);
            // 调用Service保存或更新报修人
            WxReporter wxReporter = wxReporterService.saveOrUpdate(reporter);
			WxRepair wxRepair = new WxRepair();
			// 2. 生成唯一工单号
			String workOrderNumber = "WX" + System.currentTimeMillis()
					+ UUID.randomUUID().toString().replace("-", "").substring(0, 8);
			wxRepair.setWorkOrderNumber(workOrderNumber);
			// 3. 设置业务属性
			wxRepair.setOpenId(openId); // 将OpenID存入对象
//			wxRepair.setSchoolName(schoolName);
//			wxRepair.setSchoolAddress(schoolAddress);
//			wxRepair.setCampus(campus);
//			wxRepair.setUserName(userName);
//			wxRepair.setContactNumber(contactNumber);
			wxRepair.setWxReporter(wxReporter);
			wxRepair.setReportClassroomRepair(reportClassroomRepair);
			wxRepair.setEquipmentRepair(equipmentRepair);
			wxRepair.setFaultInformation(faultInformation);
			wxRepair.setUrgencyLevel(urgencyLevel);
			wxRepair.setExpectedVisitTime(expectedVisitTime.replace("T", " "));

			// 建议在这里设置一些默认值，比如创建时间和初始状态
			wxRepair.setCreateTime(new Date());
			wxRepair.setStatus(RepairStatus.PENDING.getCode()); // 例如：待处理
			// 4. 调用Service层保存数据
			this.wxRepairService.saveOrUpdate(wxRepair, photos, imgPath, dir);
//			报修区域
//			报修人
//			报修时间
//			设备名称
//			报修类型
			//收到信息 给商务推送消息
			List<WxBinding> findBindingsByPersonnelType = this.wxBindingService.findBindingsByPersonnelType(PersonnelType.DISPATCHER);//拿到所有调度人员
			if(findBindingsByPersonnelType.size()>0) {
				Map<String, String> map = new HashMap<>();
				map.put("thing2", Common.getOrDefault(reporter.getSchoolName(), "未知学校") + "校区：" + Common.getOrDefault(reporter.getCampus(), "未知校区"));
				map.put("thing3", Common.getOrDefault(reporter.getUserName(), "未知报修人"));
				map.put("time4", Common.getDateYMDHM(wxRepair.getCreateTime()));
				map.put("thing5", Common.getOrDefault(wxRepair.getEquipmentRepair(), "未知设备类型"));
				map.put("thing1", "紧急程度：" + Common.getOrDefault(urgencyLevel, "普通"));
//				map.put("thing2", reporter.getSchoolName()+"校区："+reporter.getCampus());
//				map.put("thing3", reporter.getUserName());
//				map.put("time4", Common.getDateYMDHM(wxRepair.getCreateTime()));
//				map.put("thing5", wxRepair.getEquipmentRepair());
//				map.put("thing1", "紧急程度："+urgencyLevel);
				//执行推送
				findBindingsByPersonnelType.stream().filter(user -> Common.isNotEmpty(user.getOpenId())).forEach(user -> {
					String sendWxMessage = this.wxMsgPush.sendWxMessage(templateId4, user.getOpenId(),
							weburl + "/wechatrp/findWxRepair/" + wxRepair.getId(), map);
					log.info("用户[{}]提交报修单成功，消息推送成功：{}", openId, sendWxMessage);
				});
			}
			


			log.info("用户[{}]提交报修单成功，工单号：{}", openId, workOrderNumber);
			return new BasicDataResult().build(200, "提交成功", workOrderNumber);

		} catch (Exception e) {
			// 5. 异常处理：记录详细日志
			log.error("提交报修单时发生未知异常: ", e);
			return new BasicDataResult().build(500, "服务器内部错误，提交失败", null);
		}
	}
	
	
	
	
	@GetMapping(value = "/findWxRepairlist")
	public String findWxRepairlist(HttpServletRequest request, Model model, HttpSession session) {
	    try {
	        // 1. 先从 session 中查找是否已经有 openId
	        String openId = (String) session.getAttribute("openId");

	        if (openId == null) {
	            // 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
	            String code = request.getParameter("code");

	            if (code == null) {
	                // 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
	                String redirect_uri = weburl + "/wechatrp/operations_repairlist";
	                return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + 
	                       "appid=" + wxMpProperties.getConfigs().get(0).getAppId() + 
	                       "&redirect_uri=" + URLEncoder.encode(redirect_uri, "UTF-8") + 
	                       "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
	            }
	            
	            // 使用 code 调用微信接口获取 openId
	            WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
	            WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
	            openId = accessToken.getOpenId();

	            // 3. 将获取到的 openId 存入 session，以便后续请求使用
	            session.setAttribute("openId", openId);
	  
	        	WxOAuth2UserInfo userInfo = oAuth2Service.getUserInfo(accessToken, null);
				model.addAttribute("userInfo", userInfo);
	        } else {
	            // 4. 如果 session 中已经有 openId，说明是重复请求（如刷新），直接从 session 中获取
	            System.out.println("openId 已存在于 session 中，直接使用: " + openId);
	        }

	           
	        
	        // 此时 openId 一定是有效的，可以安全地使用它来查询数据
	        List<WxRepair> repairList = this.wxRepairService.findOperationsWxRepairByOpenId(openId,"",null,"");
	        
	        model.addAttribute("repairList", repairList);
	        model.addAttribute("openId", openId);
	        model.addAttribute("userInfo", session.getAttribute("userInfo"));

	    } catch (WxErrorException | UnsupportedEncodingException e) {
	        // 如果是因为 code 无效（如刷新导致），则清除 session 并重定向
	        if (((WxErrorException) e).getError().getErrorCode() == 40163) {
	            session.invalidate(); // 清除无效的 session
	            String redirect_uri = weburl + "/wechatrp/operations_repairlist";
	            return "redirect:" + redirect_uri; // 重定向到当前页面，会触发新的授权流程
	        }
	        e.printStackTrace();
	        // 其他错误处理...
	        return "error"; 
	    }
	    
	    return "school/repairlist";
	}

	


	
	

}