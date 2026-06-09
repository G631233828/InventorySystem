
package zhongchiedu.common.utils;

import com.baidu.aip.ocr.AipOcr;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;

public class BaiduOcrUtil {
    //填入你自己百度后台的三个密钥
	private static final String APP_ID = "7824503";
	private static final String API_KEY = "oog3R4xi3f1g4nX6tLlJIgAz";
	private static final String SECRET_KEY = "iPbhntgwwLSE5gAVyONVctsL4ZyLub74";

    private static AipOcr client;

    static {
        client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
        client.setConnectionTimeoutInMillis(5000);
        client.setSocketTimeoutInMillis(15000);
    }

    /**
     * 图片OCR识别
     */
    public static String accurateOcr(File file) {
        HashMap<String, String> options = new HashMap<>();
        options.put("detect_direction", "true");//自动转正竖排/倾斜图片
        options.put("language_type", "CHN_ENG");
        JSONObject res = client.basicGeneral(file.getAbsolutePath(), options);
        return parseResult(res);
    }

    /**
     * 接收前端上传文件
     */
    public static String accurateOcr(MultipartFile file) throws Exception {
        File tmp = File.createTempFile("ocr_temp", ".jpg");
        file.transferTo(tmp);
        String result = accurateOcr(tmp);
        tmp.delete();
        return result;
    }

    /**
     * 拼接识别文字
     */
    private static String parseResult(JSONObject json) {
        StringBuilder sb = new StringBuilder();
        if (!json.has("words_result")) {
            return "";
        }
        JSONArray array = json.getJSONArray("words_result");
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            sb.append(obj.getString("words"));
        }
        return sb.toString();
    }

    /**
     * 校验是否是维修单：核心匹配【设备维修服务单】
     */
    public static boolean isTriplicateReceipt(String ocrText) {
        if (ocrText == null || ocrText.isEmpty()) {
            return false;
        }
        //去除所有空格换行
        String content = ocrText.replaceAll("\\s+", "");
        //关键字优先匹配：设备维修服务单，附带备选关键词
        String[] keys = {"设备维修服务单", "维修服务单"};
        for (String key : keys) {
            if (content.contains(key)) {
                return true;
            }
        }
        return false;
    }

    //本地测试
    public static void main(String[] args) throws Exception {
        File img = new File("C:\\test.jpg");
        String text = accurateOcr(img);
        System.out.println("识别全文：" + text);
        boolean flag = isTriplicateReceipt(text);
        System.out.println(flag ? "✅含设备维修服务单，有效单据" : "❌非维修单据");
    }
}