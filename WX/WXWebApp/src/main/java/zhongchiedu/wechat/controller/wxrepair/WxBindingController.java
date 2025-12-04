package zhongchiedu.wechat.controller.wxrepair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.wx.config.WxMpProperties;
import zhongchiedu.wx.pojo.WXUserInfo;
import zhongchiedu.wx.service.WXUserInfoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/wechatrp")
public class WxBindingController {


	@Autowired
	private WxBindingService wxBindingService;
	
	@Autowired
	private WxMpService wxMpService;
	@Autowired
	private WxMpProperties wxMpProperties;
	
	@Value("${qrcode.weburl}")
	private String weburl;
	
	@Value("${templateId5}")
	private String templateId5; // 维修订单模版
	
	@Autowired
	private WXUserInfoService wXUserInfoService;

	
    /**
     * 跳转到绑定页面
     *
     * @param openId 从微信授权后获取的 openId
     * @param model  用于向页面传递数据
     * @return 绑定页面
     */
    @GetMapping("/toBinding")
    public String toBindingPage(HttpServletRequest request, Model model, HttpSession session) {
    		try {
    	   String openId = (String) session.getAttribute("openId");
	        if (openId == null) {
	            // 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
	            String code = request.getParameter("code");

	            if (code == null) {
	                // 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
	                String redirect_uri = weburl+ "/wechatrp/toBinding";
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
	      
	        } else {
	        	System.out.println("openId 已存在于 session 中，直接使用: " + openId);
	        
	        }
	        WxBinding wb = this.wxBindingService.findWxBindingByOpenId(openId);
            if(wb!=null) {
            	 //已经绑定成功了 ，无需重新绑定
            	   return "school/bindOK"; 
            }
	        
    		}catch (WxErrorException | UnsupportedEncodingException e) {
    	        // 如果是因为 code 无效（如刷新导致），则清除 session 并重定向
    	        if (((WxErrorException) e).getError().getErrorCode() == 40163) {
    	            session.invalidate(); // 清除无效的 session
    	            String redirect_uri = weburl+ "/wechatrp/toBinding";
    	            return "redirect:" + redirect_uri; // 重定向到当前页面，会触发新的授权流程
    	        }
    	        e.printStackTrace();
    	        // 其他错误处理...
    	        return "error"; 
    	    }
        // 将 openId 传递到页面，用于表单提交时隐藏域使用
        return "school/binding"; // 跳转到 templates 目录下的 binding.html
    }

    /**
     * 处理微信绑定逻辑
     *
     * @param wxBinding 接收表单提交的绑定信息
     * @param result    用于校验结果
     * @return 返回 JSON 格式的响应结果
     */
    @PostMapping("/wxBinding")
    @ResponseBody
    public Map<String, Object> wxBinding(@Valid @RequestBody WxBinding wxBinding, HttpServletRequest request,BindingResult result) {
    	  String openId = (String) request.getSession().getAttribute("openId");
    	  wxBinding.setOpenId(openId);
    	  
        Map<String, Object> response = new HashMap<>();
        // 校验参数
        if (result.hasErrors()) {
            response.put("status", 500);
            response.put("msg", result.getFieldError().getDefaultMessage());
            return response;
        }
        try {
            // 保存或更新绑定信息到 MongoDB
            wxBindingService.saveOrUpdate(wxBinding);
            response.put("status", 200);
            response.put("msg", "绑定成功！");
            response.put("redirectUrl", "../wechatrp/bindingSuccess");
        } catch (Exception e) {
            response.put("status", 500);
            response.put("msg", "绑定失败：" + e.getMessage());
        }

        return response;
    }
    
    @GetMapping("/bindingSuccess")
    public String bindingSuccessPage() {
        // 无需传递参数，直接返回成功页面
        return "school/bindOK";
    }
    
    
}