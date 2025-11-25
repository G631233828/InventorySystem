package zhongchiedu.wechat.controller.wxrepair;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Comparator;

@RestController
@RequestMapping("/wechatrp")
public class BaiduGeocodeController {
    // 替换为你的百度AK（确保已开启「逆地理编码」和「POI搜索」权限）
    private static final String BAIDU_AK = "Auo0Upys2oNi6lRUtEKbexipHWGQBM9f";
    // 百度地理编码API URL
    private static final String BAIDU_API_URL = "https://api.map.baidu.com/reverse_geocoding/v3/";

    /**
     * 优化后：接收经纬度，返回详细地址（优先POI名称+门牌号）
     * @param latitude 纬度（GPS坐标，如31.244484）
     * @param longitude 经度（GPS坐标，如121.62069）
     * @return 包含详细地址的JSON结果121.633936,31.248550
     */
    @GetMapping("/geocode")
    public JSONObject getAddressByLocation(
            @RequestParam(required = true) double latitude,
            @RequestParam(required = true) double longitude) {
        
        JSONObject result = new JSONObject();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try {
            // 优化参数：radius扩大到50米（覆盖GPS误差范围），确保POI搜索有效
            String url = String.format(
                    "%s?ak=%s&output=json&coordtype=bd09ll&location=%s,%s&extensions=2&radius=10",
                    BAIDU_API_URL, BAIDU_AK, latitude, longitude
            );
            
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            
            if (entity != null) {
                String responseStr = EntityUtils.toString(entity, "UTF-8");
                JSONObject baiduResult = JSONObject.parseObject(responseStr);
                
                // 解析百度返回结果（status=0表示成功）
                if (baiduResult.getInteger("status") == 0) {
                    JSONObject resultObj = baiduResult.getJSONObject("result");
                    JSONObject addressComponent = resultObj.getJSONObject("addressComponent");
                    JSONArray pois = resultObj.getJSONArray("pois"); // 附近兴趣点（大厦、小区等）

                    // 1. 提取基础结构化地址（省市区+街道）
                    String baseAddress = resultObj.getString("formatted_address");
                    // 2. 提取门牌号（街道+门牌号，如“锦绣东路123号”）
                    String street = addressComponent.getString("street"); // 街道（锦绣东路）
                    String streetNumber = addressComponent.getString("street_number"); // 门牌号（如1688号）
                    String detailStreetAddress = street;
                    if (streetNumber != null && !streetNumber.isEmpty()) {
                        detailStreetAddress += streetNumber; // 组合为“锦绣东路1688号”
                    }

                    // 3. 提取最近的POI名称（如“怡亚通广场”）
                    String poiName = null;
                    if (pois != null && pois.size() > 0) {
                        // 按距离排序，取最近的POI
                        poiName = pois.stream()
                                .map(poi -> (JSONObject) poi)
                                .min(Comparator.comparingInt(poi -> poi.getInteger("distance")))
                                .map(poi -> poi.getString("name"))
                                .orElse(null);
                    }

                    // 组合最终详细地址（优先级：POI名称 > 街道+门牌号 > 基础地址）
                    String finalAddress = baseAddress;
                    if (poiName != null && !poiName.isEmpty()) {
                        finalAddress = poiName + "（" + detailStreetAddress + "）"; // 如“怡亚通广场（锦绣东路1688号）”
                    } else if (!detailStreetAddress.equals(street)) { // 有门牌号时
                        finalAddress = detailStreetAddress; // 如“锦绣东路1688号”
                    }

                    // 返回结果（包含不同粒度的地址，方便前端选择）
                    result.put("status", 200);
                    result.put("message", "地址解析成功");
                    result.put("baseAddress", baseAddress); // 基础地址（省市区+街道）
                    result.put("detailStreetAddress", detailStreetAddress); // 街道+门牌号
                    result.put("poiName", poiName); // 最近POI名称（大厦/小区）
                    result.put("address", finalAddress); // 最终推荐详细地址

                } else {
                    result.put("status", 500);
                    result.put("message", "百度API解析失败：" + baiduResult.getString("message"));
                }
            }
            
            response.close();
        } catch (IOException e) {
            result.put("status", 500);
            result.put("message", "请求百度API失败：" + e.getMessage());
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return result;
    }
}