package zhongchiedu.wechat.controller.wxrepair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import zhongchiedu.common.utils.enums.PersonJoinAuditStatusEnum;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.inventory.service.WxReporterService;
import zhongchiedu.wx.config.WxMpProperties;
@Controller
@RequestMapping("/wechatrp")
public class WxRepairToOperListController {

	@Autowired
	private WxRepairService wxRepairService;
	
	@Autowired
	private WxReporterService wxReporterService;
	


	@Autowired
	private WxMpService wxMpService;
	@Autowired
	private WxMpProperties wxMpProperties;
	
	@Value("${qrcode.weburl}")
	private String weburl;
	
	@Autowired
	private WxBindingService wxBindingService;
	
	/**
	 *  调度跟维修人员  operator_repairlist  查看所有报修信息
	 * @param request
	 * @param model
	 * @param session
	 * @param status
	 * @param dateRange
	 * @param search
	 * @return
	 */
//	@GetMapping(value = "/operator_repairlist")
//	public String operator_repairlist(HttpServletRequest request, Model model, HttpSession session,String search,String status,String workerId) {
//		String url ="";
//	    try {
//	        // 1. 先从 session 中查找是否已经有 openId
//	        String openId = (String) session.getAttribute("openId");
//
//	        if (openId == null) {
//	            // 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
//	            String code = request.getParameter("code");
//
//	            if (code == null) {
//	                // 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
//	                String redirect_uri = weburl + "/wechatrp/operator_repairlist";
//	                return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + 
//	                       "appid=" + wxMpProperties.getConfigs().get(0).getAppId() + 
//	                       "&redirect_uri=" + URLEncoder.encode(redirect_uri, "UTF-8") + 
//	                       "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
//	            }
//	            
//	            // 使用 code 调用微信接口获取 openId
//	            WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
//	            WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
//	            openId = accessToken.getOpenId();
//	            // 3. 将获取到的 openId 存入 session，以便后续请求使用
//	            session.setAttribute("openId", openId);
//	          
//	          
//	        } else {
//	            // 4. 如果 session 中已经有 openId，说明是重复请求（如刷新），直接从 session 中获取
//	            System.out.println("openId 已存在于 session 中，直接使用: " + openId);
//	        }
//	        //根据openId去wxbinding中查询
//	        WxBinding wx = this.wxBindingService.findWxBindingByOpenId(openId);
//            if(wx == null) {
//            	//跳转绑定界面
//            	  return "school/binding";
//            }
//            
//         
//            
//	        Integer personnelType = wx.getPersonnelType();
//            if(PersonnelType.CONSTRUCTION_TEAM.getCode().equals(personnelType)) {
//            	url = "school/workerRepairList";
//            }else if(PersonnelType.DISPATCHER.getCode().equals(personnelType)) {
//            	   //获取所有维修人员
//                List<WxBinding> workers = this.wxBindingService.findBindingsByPersonnelType(PersonnelType.CONSTRUCTION_TEAM);
//            	url = "school/diaoduRepairList";
//            	model.addAttribute("workers", workers);
//            }
//            
//            // 可以顺便把用户信息也存入 session
//            WxMpUser userInfo = wxMpService.getUserService().userInfo(openId);
//            session.setAttribute("userInfo", userInfo);
//	        List<WxRepair> repairList = wxRepairService.findOperationsWxRepairByOpenId(openId);
//	        model.addAttribute("repairList", repairList);
//	        model.addAttribute("openId", openId);
//	        model.addAttribute("userInfo", session.getAttribute("userInfo"));
//
//	    } catch (WxErrorException | UnsupportedEncodingException e) {
//	        // 如果是因为 code 无效（如刷新导致），则清除 session 并重定向
//	        if (((WxErrorException) e).getError().getErrorCode() == 40163) {
//	            session.invalidate(); // 清除无效的 session
//	            String redirect_uri = weburl + "/wechatrp/toBinding";
//	            return "redirect:" + redirect_uri; // 重定向到当前页面，会触发新的授权流程
//	        }
//	        e.printStackTrace();
//	        // 其他错误处理...
//	        return "error"; 
//	    }
//	    
//	    return url;
//	}
	

	@GetMapping(value = "/operator_repairlist")
	public String operator_repairlist(HttpServletRequest request, Model model, HttpSession session,
	                                 @RequestParam(required = false) String search,
	                                 @RequestParam(required = false) String status,
	                                 @RequestParam(required = false) String workerId) {
	    String url = "";
	    String openId = (String) session.getAttribute("openId");
	    try {
	        // 1. 先从 session 中查找是否已经有 openId

	        if (openId == null) {
	            // 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
	            String code = request.getParameter("code");

	            if (code == null) {
//	                // 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
//	                String redirect_uri = weburl + "/wechatrp/operator_repairlist";
//	                return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" +
//	                        "appid=" + wxMpProperties.getConfigs().get(0).getAppId() +
//	                        "&redirect_uri=" + URLEncoder.encode(redirect_uri, "UTF-8") +
//	                        "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
	            	 // 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
	                // 构建带查询参数的redirect_uri
	                StringBuilder redirectUriBuilder = new StringBuilder(weburl + "/wechatrp/operator_repairlist");
	                boolean hasParams = false;
	                
	                // 添加查询参数
	                if (StringUtils.hasText(search)) {
	                    redirectUriBuilder.append(hasParams ? "&" : "?").append("search=").append(URLEncoder.encode(search, "UTF-8"));
	                    hasParams = true;
	                }
	                if (StringUtils.hasText(status)) {
	                    redirectUriBuilder.append(hasParams ? "&" : "?").append("status=").append(status);
	                    hasParams = true;
	                }
	                if (StringUtils.hasText(workerId)) {
	                    redirectUriBuilder.append(hasParams ? "&" : "?").append("workerId=").append(workerId);
	                    hasParams = true;
	                }
	                
	                String redirect_uri = redirectUriBuilder.toString();
	                
	                return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" +
	                        "appid=" + wxMpProperties.getConfigs().get(0).getAppId() +
	                        "&redirect_uri=" + URLEncoder.encode(redirect_uri, "UTF-8") +
	                        "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
	            }

	            // 使用 code 调用微信接口获取 openId
	            WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
	            WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
	            // 3. 将获取到的 openId 存入 session，以便后续请求使用
	            openId = accessToken.getOpenId();

	            
	            session.setAttribute("openId", openId);
	        } else {
	            // 4. 如果 session 中已经有 openId，说明是重复请求（如刷新），直接从 session 中获取
	            System.out.println("openId 已存在于 session 中，直接使用: " + openId);
	        }

	        // 根据openId去wxbinding中查询
	        WxBinding wx = this.wxBindingService.findWxBindingByOpenId(openId);
	        if (wx == null) {
	            // 跳转绑定界面
	            return "school/error";
	        }
	        if(wx.getAuditStatus().equals(PersonJoinAuditStatusEnum.REFUSE.getCode())){
	        	return "school/audit";
	        }

	        Integer personnelType = wx.getPersonnelType();
	        List<WxRepair> repairList = null;
	        
	        // 处理查询参数类型转换
	        Integer statusInt = null;
	        if (StringUtils.hasText(status)) {
	            try {
	                statusInt = Integer.parseInt(status);
	            } catch (NumberFormatException e) {
	                statusInt = null;
	            }
	        }



	        if (PersonnelType.CONSTRUCTION_TEAM.getCode().equals(personnelType)) {
	            // 维修人员：只能查看自己的工单，根据search、status筛选
	            repairList = wxRepairService.findOperationsWxRepairByOpenId(openId, search, statusInt,"");
	            url = "school/workerRepairList";
	        } else if (PersonnelType.DISPATCHER.getCode().equals(personnelType)) {
	            // 调度人员：可以根据search、status、workerId筛选所有工单
	            List<WxBinding> workers = this.wxBindingService.findBindingsByPersonnelType(PersonnelType.CONSTRUCTION_TEAM);
	            model.addAttribute("workers", workers);
	            
	            repairList = wxRepairService.findOperationsWxRepairByOpenId(openId,search, statusInt, workerId);
	            url = "school/diaoduRepairList";
	        }

	        // 可以顺便把用户信息也存入 session
	        WxMpUser userInfo = wxMpService.getUserService().userInfo(openId);
	        session.setAttribute("userInfo", userInfo);

	        // 将查询条件返回给页面用于回显
	        model.addAttribute("repairList", repairList);
	        model.addAttribute("openId", openId);
	        model.addAttribute("userInfo", userInfo);
	        model.addAttribute("search", search);       // 搜索关键词
	        model.addAttribute("status", status);       // 状态筛选条件
	        model.addAttribute("workerId", workerId);   // 维修人员筛选条件

	    } catch (WxErrorException | UnsupportedEncodingException e) {
	        // 如果是因为 code 无效（如刷新导致），则清除 session 并重定向
	        if (((WxErrorException) e).getError().getErrorCode() == 40163) {
	            session.invalidate(); // 清除无效的 session
	            String redirect_uri = weburl + "/wechatrp/toBinding";
	            return "redirect:" + redirect_uri; // 重定向到当前页面，会触发新的授权流程
	        }
	        e.printStackTrace();
	        // 其他错误处理...
	        return "error";
	    }

	    return url;
	}
	
	
	
}
