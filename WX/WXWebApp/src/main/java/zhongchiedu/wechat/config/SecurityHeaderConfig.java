package zhongchiedu.wechat.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityHeaderConfig {

    @Bean
    public OncePerRequestFilter securityHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                    FilterChain filterChain) throws ServletException, IOException {
                // 启用XSS防护（IE/Chrome）
                response.setHeader("X-XSS-Protection", "1; mode=block");
                // 禁止页面嵌入到iframe（防点击劫持）
                response.setHeader("X-Frame-Options", "DENY");
                // 限制资源加载（防跨站脚本）
               // response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; style-src 'self' https://cdn.jsdelivr.net; img-src 'self' data:;");
                // 禁止MIME类型嗅探
                response.setHeader("X-Content-Type-Options", "nosniff");
                
                filterChain.doFilter(request, response);
            }
        };
    }
}