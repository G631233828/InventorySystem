package zhongchiedu.wechat.compent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import zhongchiedu.inventory.service.WxReporterService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 报修系统OpenID黑名单拦截器
 * 基于WxReporter的isBlocked字段拦截
 */
@Component
public class BlacklistInterceptor implements HandlerInterceptor {

    @Autowired
    private WxReporterService wxReporterService;

    /**
     * 请求处理前执行拦截逻辑
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取微信OpenID（根据你的系统实际情况调整）
        String openid = request.getHeader("X-WX-OpenID") != null 
                ? request.getHeader("X-WX-OpenID") 
                : request.getParameter("openid");

        // 2. 检查OpenID是否被拉黑
        if (openid != null && wxReporterService.isOpenidBlocked(openid)) {
            return handleBlockedRequest(response, "你的账号因恶意报修已被限制使用");
        }

        // 3. 未命中黑名单，放行请求
        return true;
    }

    /**
     * 处理被拦截的请求，返回友好提示
     */
    private boolean handleBlockedRequest(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403禁止访问
        PrintWriter writer = response.getWriter();
        writer.write("{\"code\":403,\"msg\":\"" + message + "\",\"data\":null}");
        writer.flush();
        writer.close();
        return false; // 阻止请求继续处理
    }
}