package zhongchiedu.log.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.gson.Gson;

import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.general.pojo.User;
import zhongchiedu.log.annotation.SystemControllerLog;
import zhongchiedu.log.annotation.SystemServiceLog;
import zhongchiedu.log.dao.LogDaoImpl;
import zhongchiedu.log.pojo.Log;

@Aspect
@Component
public class SystemLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(SystemLogAspect.class);

    @Autowired
    private LogDaoImpl logDao; // 变量名规范：小写开头

    // Service层切点
    @Pointcut("@annotation(zhongchiedu.log.annotation.SystemServiceLog)")
    public void serviceAspect() {}

    // Controller层切点
    @Pointcut("@annotation(zhongchiedu.log.annotation.SystemControllerLog)")
    public void controllerAspect() {}

    /**
     * 前置通知：记录正常操作日志
     */
    @Before("controllerAspect()")
    public void doBefore(JoinPoint joinPoint) {
        // 1. 获取请求上下文（避免空指针）
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            logger.warn("前置通知：无法获取请求上下文，跳过日志记录");
            return;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        HttpSession session = request.getSession(false); // 避免创建新session
        if (session == null) {
            logger.warn("前置通知：session不存在，跳过日志记录");
            return;
        }

        // 2. 获取用户信息（避免空指针）
        User user = (User) session.getAttribute(Contents.USER_SESSION);
        if (user == null) {
            logger.warn("前置通知：未获取到登录用户，跳过日志记录");
            return;
        }

        try {
            // 3. 构建日志信息
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            String fullMethod = className + "." + methodName + "()";
            String description = getControllerMethodDescription(joinPoint);
            String ip = request.getRemoteAddr();

            // 4. 控制台输出（简洁化，重要信息用logger）
            logger.info("=====前置通知开始=====");
            logger.info("请求方法: {}", fullMethod);
            logger.info("方法描述: {}", description);
            logger.info("请求IP: {}", ip);
            logger.info("操作人: {}", user.getUserName());

            // 5. 保存数据库
            Log log = new Log();
            log.setDescription(description);
            log.setMethod(fullMethod);
            log.setType("0"); // 正常操作
            log.setRequestIp(ip);
            log.setCreateby(user.getUserName());
            log.setCreateDate(Common.fromDateH());
            logDao.insert(log); // 变量名修正

            logger.info("=====前置通知结束=====");
        } catch (Exception e) {
            // 异常日志标准化：包含上下文信息
            logger.error("前置通知记录日志失败！请求方法: {}", 
                    joinPoint.getSignature().toShortString(), e);
        }
    }

    /**
     * 异常通知：记录异常操作日志
     */
    @AfterThrowing(pointcut = "controllerAspect()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable e) {
        // 1. 获取请求上下文（避免空指针）
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            logger.warn("异常通知：无法获取请求上下文，跳过日志记录");
            return;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.warn("异常通知：session不存在，跳过日志记录");
            return;
        }

        // 2. 获取用户信息
        User user = (User) session.getAttribute(Contents.USER_SESSION);
        String operator = (user != null) ? user.getAccountName() : "未知用户"; // 避免空指针

        try {
            // 3. 构建日志信息
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            String fullMethod = className + "." + methodName + "()";
            String description = getControllerMethodDescription(joinPoint);
            String ip = request.getRemoteAddr();

            // 4. 处理请求参数（避免Gson序列化异常）
            Gson gson = new Gson();
            String params = Arrays.stream(joinPoint.getArgs())
                    .map(arg -> {
                        try {
                            return gson.toJson(arg);
                        } catch (Exception ex) {
                            logger.warn("参数序列化失败：{}", arg.getClass().getSimpleName(), ex);
                            return "参数序列化失败";
                        }
                    })
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("无参数");

            // 5. 控制台输出（用logger替代System.out，便于日志收集）
            logger.error("=====异常通知开始=====", e); // 打印异常堆栈
            logger.error("异常方法: {}", fullMethod);
            logger.error("方法描述: {}", description);
            logger.error("操作人: {}", operator);
            logger.error("请求IP: {}", ip);
            logger.error("请求参数: {}", params);
            logger.error("异常类型: {}", e.getClass().getName());
            logger.error("异常信息: {}", e.getMessage());
            logger.error("=====异常通知结束=====");

            // 6. 保存数据库
            Log log = new Log();
            log.setDescription(description);
            log.setMethod(fullMethod);
            log.setType("1"); // 异常操作
            log.setRequestIp(ip);
            log.setParams(params);
            log.setExceptionCode(e.getClass().getName());
            log.setExceptionDetail(e.getMessage());
            log.setCreateby(operator);
            log.setCreateDate(Common.fromDateH());
            logDao.insert(log);

        } catch (Exception ex) {
            // 异常日志标准化
            logger.error("异常通知记录日志失败！请求方法: {}", 
                    joinPoint.getSignature().toShortString(), ex);
        }
    }

    /**
     * 获取Controller方法的注解描述
     */
    public static String getControllerMethodDescription(JoinPoint joinPoint) throws Exception {
        String targetName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] arguments = joinPoint.getArgs();

        Class<?> targetClass = Class.forName(targetName);
        Method[] methods = targetClass.getMethods();

        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (Arrays.equals(parameterTypes, getArgumentTypes(arguments))) { // 更严谨的参数匹配
                    SystemControllerLog annotation = method.getAnnotation(SystemControllerLog.class);
                    if (annotation != null) {
                        return annotation.description();
                    }
                }
            }
        }
        return "未获取到方法描述";
    }

    /**
     * 获取Service方法的注解描述（复用逻辑）
     */
    public static String getServiceMthodDescription(JoinPoint joinPoint) throws Exception {
        String targetName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] arguments = joinPoint.getArgs();

        Class<?> targetClass = Class.forName(targetName);
        Method[] methods = targetClass.getMethods();

        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (Arrays.equals(parameterTypes, getArgumentTypes(arguments))) {
                    SystemServiceLog annotation = method.getAnnotation(SystemServiceLog.class);
                    if (annotation != null) {
                        return annotation.description();
                    }
                }
            }
        }
        return "未获取到方法描述";
    }

    /**
     * 辅助方法：获取参数类型数组（用于匹配方法）
     */
    private static Class<?>[] getArgumentTypes(Object[] arguments) {
        if (arguments == null) {
            return new Class<?>[0];
        }
        return Arrays.stream(arguments)
                .map(arg -> arg != null ? arg.getClass() : null)
                .toArray(Class<?>[]::new);
    }
}