//package zhongchiedu.wechat.compent;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
//
//import zhongchiedu.inventory.pojo.WxBinding;
//import zhongchiedu.inventory.service.WxBindingService;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * 微信绑定用户权限拦截器
// * 拦截非白名单页面，校验用户审核状态
// */
//@Component
//public class WxAuthInterceptor implements HandlerInterceptor {
//
//    // 白名单路径（不拦截的页面）
//    private static final List<String> WHITE_LIST = Arrays.asList(
//            "/wechatrp/toBinding",
//            "/wechatrp/wxBinding",
//            "/wechatrp/repairlist",
//            "/wechatrp/operator_repairlist",//维修人员进入list页面
//            "/wechatrp/repair",
//            "/wechatrp/geocode",
//            "/wechatrp/wxRepair",
//            "/wechatrp/toAudio"//跳转未授权界面
//    );
//
//    // 未授权跳转页面
//    private static final String UNAUTHORIZED_PAGE = "/wechatrp/toAudio";
//
//    @Autowired
//    private WxBindingService wxBindingService; // 注入WxBinding业务层
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        String requestURI = request.getRequestURI();
//        String contextPath = request.getContextPath();
//        
//        // 去除上下文路径，获取相对路径
//        String path = requestURI.replace(contextPath, "").replaceAll("//+", "/");
//
//        // 1. 判断是否是白名单路径，直接放行
//        if (WHITE_LIST.contains(path)) {
//            return true;
//        }
//
//        // 2. 获取当前用户的openId（假设从Session中获取，根据实际登录方式调整）
//        HttpSession session = request.getSession(false);
//        String openId = session != null ? (String) session.getAttribute("openId") : null;
//        
//        // 无openId，跳转到未授权页面
//        if (openId == null || openId.trim().isEmpty()) {
//            response.sendRedirect(contextPath + UNAUTHORIZED_PAGE);
//            return false;
//        }
//
//        // 3. 查询用户绑定信息
//        WxBinding wxBinding = wxBindingService.findWxBindingByOpenId(openId); // 需实现根据openId查询的方法
//        
//        // 4. 校验审核状态（必须为2：通过）
//        if (wxBinding == null || wxBinding.getAuditStatus() == null || wxBinding.getAuditStatus() != 2) {
//            response.sendRedirect(contextPath + UNAUTHORIZED_PAGE);
//            return false;
//        }
//
//        // 5. 审核通过，放行
//        return true;
//    }
//
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
//    }
//}