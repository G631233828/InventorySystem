package zhongchiedu.wechat.controller.wxrepair;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

@Slf4j
@RestController
@RequestMapping("/wechatrp")
public class BaiduGeocodeController {
    // 百度地图AK
    private static final String BAIDU_AK = "Auo0Upys2oNi6lRUtEKbexipHWGQBM9f";
    // 百度逆地理编码API
    private static final String BAIDU_REVERSE_GEO_URL = "https://api.map.baidu.com/reverse_geocoding/v3/";
    // 百度坐标转换API（WGS84→BD09）
    private static final String BAIDU_COORD_TRANS_URL = "https://api.map.baidu.com/geoconv/v1/";
    // POI类型常量，包含特殊符号|
    private static final String POI_TYPE = "教育|学校";

    /**
     * 高精度地址解析：1.坐标转换 2.优先筛选学校POI 3.精准地址组合
     * @param latitude WGS84纬度
     * @param longitude WGS84经度
     * @return 高精度地址结果
     */
    @GetMapping("/geocode")
    public JSONObject getAddressByLocation(
            @RequestParam(required = true) double latitude,
            @RequestParam(required = true) double longitude) {

        JSONObject result = new JSONObject();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 1. 坐标转换：WGS84(GPS) → BD09(百度坐标)
            JSONObject transResult = convertCoord(httpClient, longitude, latitude);
            if (transResult == null || transResult.getInteger("status") != 0) {
                result.put("status", 500);
                result.put("message", "坐标转换失败：" + (transResult != null ? transResult.getString("message") : "未知错误"));
                return result;
            }

            JSONArray transPoints = transResult.getJSONArray("result");
            double bdLng = transPoints.getJSONObject(0).getDouble("x"); // 百度经度
            double bdLat = transPoints.getJSONObject(0).getDouble("y"); // 百度纬度

            // ========== 修复：对带|的参数URL编码 ==========
            String encodePoiType = URLEncoder.encode(POI_TYPE, StandardCharsets.UTF_8.name());
            String geoUrl = String.format(
                    "%s?ak=%s&output=json&coordtype=bd09ll&location=%s,%s&extensions=2&radius=100&pois=1&poi_types=%s",
                    BAIDU_REVERSE_GEO_URL, BAIDU_AK, bdLat, bdLng, encodePoiType
            );
            log.info("百度逆地理编码请求地址：{}", geoUrl);

            HttpGet httpGet = new HttpGet(geoUrl);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseStr = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    JSONObject baiduResult = JSONObject.parseObject(responseStr);

                    if (baiduResult.getInteger("status") == 0) {
                        JSONObject resultObj = baiduResult.getJSONObject("result");
                        JSONObject addressComponent = resultObj.getJSONObject("addressComponent");
                        JSONArray pois = resultObj.getJSONArray("pois");

                        // 基础地址拼接
                        String baseAddress = resultObj.getString("formatted_address");
                        String street = addressComponent.getString("street");
                        String streetNumber = addressComponent.getString("street_number");
                        String detailStreetAddress = street + (streetNumber != null && !streetNumber.isEmpty() ? streetNumber : "");

                        // 优先筛选学校/教育类POI，按距离排序
                        String poiName = null;
                        if (pois != null && pois.size() > 0) {
                            poiName = pois.stream()
                                    .map(poi -> (JSONObject) poi)
                                    .filter(poi -> poi.getString("type").contains("教育") || poi.getString("type").contains("学校"))
                                    .min(Comparator.comparingInt(poi -> poi.getInteger("distance")))
                                    .map(poi -> poi.getString("name"))
                                    .orElseGet(() -> pois.stream()
                                            .map(poi -> (JSONObject) poi)
                                            .min(Comparator.comparingInt(poi -> poi.getInteger("distance")))
                                            .map(poi -> poi.getString("name"))
                                            .orElse(null));
                        }

                        // 最终地址组合（优先级：学校POI > 街道门牌号 > 基础地址）
                        String finalAddress = baseAddress;
                        if (poiName != null && !poiName.isEmpty()) {
                            finalAddress = poiName + "（" + detailStreetAddress + "）";
                        } else if (!detailStreetAddress.equals(street)) {
                            finalAddress = detailStreetAddress;
                        }

                        // 返回完整结果
                        result.put("status", 200);
                        result.put("message", "地址解析成功");
                        result.put("originalLng", longitude);
                        result.put("originalLat", latitude);
                        result.put("bdLng", bdLng);
                        result.put("bdLat", bdLat);
                        result.put("baseAddress", baseAddress);
                        result.put("detailStreetAddress", detailStreetAddress);
                        result.put("poiName", poiName);
                        result.put("address", finalAddress);

                    } else {
                        result.put("status", 500);
                        result.put("message", "百度API解析失败：" + baiduResult.getString("message"));
                    }
                }
            }
        } catch (IOException e) {
            log.error("请求百度逆地理编码接口异常", e);
            result.put("status", 500);
            result.put("message", "请求百度API失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 坐标转换工具：WGS84 → BD09
     */
    private JSONObject convertCoord(CloseableHttpClient httpClient, double lng, double lat) throws IOException {
        String transUrl = String.format(
                "%s?ak=%s&coords=%s,%s&from=1&to=5&output=json",
                BAIDU_COORD_TRANS_URL, BAIDU_AK, lng, lat
        );
        HttpGet httpGet = new HttpGet(transUrl);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            JSONObject transResult = null;
            if (entity != null) {
                String responseStr = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                transResult = JSONObject.parseObject(responseStr);
            }
            return transResult;
        }
    }
}