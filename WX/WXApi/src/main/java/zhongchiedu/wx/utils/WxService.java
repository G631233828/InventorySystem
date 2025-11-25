//package zhongchiedu.wx.utils;
//import com.alibaba.fastjson.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import javax.annotation.Resource;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.locks.ReentrantLock;
//
///**
// * 微信服务类（获取access_token和jsapi_ticket）
// */
//@Service
//public class WxService {
//
//    // 微信接口URL常量
//    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
//    private static final String JSAPI_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi";
//
//    // 缓存相关（实际项目建议用Redis）
//    private final Map<String, String> cache = new HashMap<>();
//    private final Map<String, Long> expireTime = new HashMap<>();
//    private final ReentrantLock lock = new ReentrantLock(); // 防止并发重复请求
//
//	@Value("${wx.mp.configs[0].appId}")
//	private String appId;
//	@Value("${wx.mp.configs[0].secret}")
//	private String appSecret;
//    @Resource
//    private RestTemplate restTemplate;
//
//    /**
//     * 获取access_token（有效期7200秒，自动缓存）
//     * 文档：https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html
//     */
//    public String getAccessToken() {
//        // 1. 检查缓存是否有效
//        String cacheKey = "access_token";
//        if (isCacheValid(cacheKey)) {
//            return cache.get(cacheKey);
//        }
//
//        // 2. 加锁防止并发重复请求
//        lock.lock();
//        try {
//            // 双重检查缓存
//            if (isCacheValid(cacheKey)) {
//                return cache.get(cacheKey);
//            }
//
//            // 3. 调用微信接口获取access_token
//            String url = String.format(ACCESS_TOKEN_URL, appId, appSecret);
//            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
//            JSONObject result = JSONObject.parseObject(response.getBody());
//
//            // 4. 处理返回结果
//            if (result.containsKey("errcode")) {
//                int errCode = result.getInteger("errcode");
//                String errMsg = result.getString("errmsg");
//                throw new RuntimeException("获取access_token失败：" + errCode + "，" + errMsg);
//            }
//
//            String accessToken = result.getString("access_token");
//            int expiresIn = result.getInteger("expires_in"); // 有效期（秒）
//
//            // 5. 缓存（提前200秒过期，避免临界点失效）
//            cache.put(cacheKey, accessToken);
//            expireTime.put(cacheKey, System.currentTimeMillis() + (expiresIn - 200) * 1000L);
//            return accessToken;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    /**
//     * 获取jsapi_ticket（有效期7200秒，自动缓存）
//     * 文档：https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/JS-SDK.html#61
//     */
//    public String getJsapiTicket(String accessToken) {
//        // 1. 检查缓存是否有效
//        String cacheKey = "jsapi_ticket";
//        if (isCacheValid(cacheKey)) {
//            return cache.get(cacheKey);
//        }
//
//        // 2. 加锁防止并发重复请求
//        lock.lock();
//        try {
//            // 双重检查缓存
//            if (isCacheValid(cacheKey)) {
//                return cache.get(cacheKey);
//            }
//
//            // 3. 调用微信接口获取jsapi_ticket
//            String url = String.format(JSAPI_TICKET_URL, accessToken);
//            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
//            JSONObject result = JSONObject.parseObject(response.getBody());
//
//            // 4. 处理返回结果
//            if (result.getInteger("errcode") != 0) {
//                int errCode = result.getInteger("errcode");
//                String errMsg = result.getString("errmsg");
//                throw new RuntimeException("获取jsapi_ticket失败：" + errCode + "，" + errMsg);
//            }
//
//            String jsapiTicket = result.getString("ticket");
//            int expiresIn = result.getInteger("expires_in"); // 有效期（秒）
//
//            // 5. 缓存（提前200秒过期）
//            cache.put(cacheKey, jsapiTicket);
//            expireTime.put(cacheKey, System.currentTimeMillis() + (expiresIn - 200) * 1000L);
//            return jsapiTicket;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    /**
//     * 检查缓存是否有效
//     */
//    private boolean isCacheValid(String key) {
//        return cache.containsKey(key) 
//                && expireTime.containsKey(key) 
//                && System.currentTimeMillis() < expireTime.get(key);
//    }
//}