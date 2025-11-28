package zhongchiedu.wechat.compent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class WeChatFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(WeChatFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String userAgent = req.getHeader("User-Agent");
        // 校验 User-Agent 是否包含微信标识
        if (userAgent == null || !userAgent.contains("MicroMessenger")) {
            logger.warn("非微信浏览器访问被拒绝：{}", req.getRequestURI());

            // 1. 设置响应状态码为 403 Forbidden
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            // 2. 设置响应内容类型为 HTML
            resp.setContentType("text/html;charset=UTF-8");

            PrintWriter out = resp.getWriter();
            // 3. 写入一个完整的 HTML 页面
            out.write("<!DOCTYPE html>\n" +
                    "<html lang=\"zh-CN\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <title>请在微信客户端中访问</title>\n" +
                    "    <style>\n" +
                    "        * {\n" +
                    "            margin: 0;\n" +
                    "            padding: 0;\n" +
                    "            box-sizing: border-box;\n" +
                    "        }\n" +
                    "\n" +
                    "        body {\n" +
                    "            font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif;\n" +
                    "            background-color: #f8f9fa;\n" +
                    "            display: flex;\n" +
                    "            justify-content: center;\n" +
                    "            align-items: center;\n" +
                    "            height: 100vh;\n" +
                    "            text-align: center;\n" +
                    "            padding: 20px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .container {\n" +
                    "            background-color: #fff;\n" +
                    "            border-radius: 12px;\n" +
                    "            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);\n" +
                    "            padding: 40px 30px;\n" +
                    "            max-width: 500px;\n" +
                    "            width: 100%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .wechat-icon {\n" +
                    "            font-size: 80px;\n" +
                    "            color: #7BB32E; /* 微信绿 */\n" +
                    "            margin-bottom: 25px;\n" +
                    "        }\n" +
                    "\n" +
                    "        h1 {\n" +
                    "            color: #333;\n" +
                    "            font-size: 24px;\n" +
                    "            margin-bottom: 15px;\n" +
                    "        }\n" +
                    "\n" +
                    "        p {\n" +
                    "            color: #666;\n" +
                    "            font-size: 16px;\n" +
                    "            line-height: 1.6;\n" +
                    "            margin-bottom: 30px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .guide {\n" +
                    "            background-color: #f5f5f5;\n" +
                    "            border-left: 4px solid #7BB32E;\n" +
                    "            padding: 15px;\n" +
                    "            text-align: left;\n" +
                    "            border-radius: 4px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .guide p {\n" +
                    "            margin-bottom: 8px;\n" +
                    "            font-size: 14px;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"wechat-icon\">\n" +
                    "            <svg viewBox=\"0 0 1024 1024\" width=\"1em\" height=\"1em\" fill=\"currentColor\">\n" +
                    "                <path d=\"M894.363 141.891c-25.26 0-47.455 12.63-63.484 32.344-15.82 19.485-23.91 43.28-23.91 67.709 0 77.945 42.045 132.909 103.636 163.636 8.182 4.091 17.636 4.545 26.545 1.364 10.455-3.636 17.818-12.636 21.091-23.182.455-1.364.909-2.727.909-4.091 0-26.545-13.5-48.182-39.818-63.636-12.636-6.818-26.545-9.545-41.455-9.545-52.364 0-94.182 41.455-94.182 92.727 0 25.273 8.545 48.182 25.273 67.709 16.545 19.5 38.636 29.273 62.727 29.273 26.182 0 49.091-12.273 65.455-35.273 16.545-23.182 24.818-47.455 24.818-72.727 0-82.273-45.818-141.818-109.091-175.636-8.545-4.545-18.091-5.455-27.636-2.273-10.909 3.636-18.636 12.636-22.091 23.182-.455 1.364-.909 2.727-.909 4.091 0 26.545 13.5 48.182 39.818 63.636 12.636 6.818 26.545 9.545 41.455 9.545 52.364 0 94.182-41.455 94.182-92.727 0-25.636-8.636-48.545-25.364-68.182-16.545-19.5-38.636-29.273-62.727-29.273zm-402.182 72.545c-60.545 0-109.455 48.909-109.455 109.455 0 60.545 48.909 109.455 109.455 109.455 60.545 0 109.455-48.909 109.455-109.455 0-60.545-48.909-109.455-109.455-109.455zm0 179.091c-38.545 0-69.818-31.273-69.818-69.818 0-38.545 31.273-69.818 69.818-69.818 38.545 0 69.818 31.273 69.818 69.818 0 38.545-31.273 69.818-69.818 69.818z\" fill=\"currentColor\"></path>\n" +
                    "                <path d=\"M512 894.109c-105.091 0-198.545-41.455-265.091-118.364-66.545-76.909-99.818-170.364-99.818-270.273 0-105.091 41.455-198.545 118.364-265.091 76.909-66.545 170.364-99.818 270.273-99.818 105.091 0 198.545 41.455 265.091 118.364 66.545 76.909 99.818 170.364 99.818 270.273 0 105.091-41.455 198.545-118.364 265.091-76.909 66.545-170.364 99.818-270.273 99.818z\" fill=\"currentColor\"></path>\n" +
                    "            </svg>\n" +
                    "        </div>\n" +
                    "        <h1>请在微信客户端中访问</h1>\n" +
                    "        <p>为了保证您能够正常使用所有功能，请务必在微信中打开。</p>\n" +     
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>");
            out.flush();
            out.close();
            return;
        }

        chain.doFilter(request, response); // 放行微信浏览器
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化时执行，这里无需操作
    }

    @Override
    public void destroy() {
        // 销毁时执行，这里无需操作
    }
}