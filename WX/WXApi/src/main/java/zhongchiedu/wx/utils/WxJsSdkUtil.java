//package zhongchiedu.wx.utils;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.Arrays;
//import java.util.Map;
//
///**
// * 微信JS-SDK签名工具类
// */
//public class WxJsSdkUtil {
//
//    /**
//     * 生成微信JS-SDK签名
//     * 签名算法：https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/JS-SDK.html#62
//     *
//     * @param jsapiTicket 微信JSAPI票据
//     * @param nonceStr    随机字符串
//     * @param timestamp   时间戳（秒）
//     * @param url         当前页面URL（不包含#及其后面部分）
//     * @return 签名结果
//     */
//    public static String generateSignature(String jsapiTicket, String nonceStr, long timestamp, String url) {
//        try {
//            // 1. 按字典序排序参数
//            String[] paramArr = new String[]{
//                "jsapi_ticket=" + jsapiTicket,
//                "noncestr=" + nonceStr,
//                "timestamp=" + timestamp,
//                "url=" + url
//            };
//            Arrays.sort(paramArr);
//
//            // 2. 拼接为字符串
//            StringBuilder sb = new StringBuilder();
//            for (String param : paramArr) {
//                sb.append(param).append("&");
//            }
//            String string1 = sb.substring(0, sb.length() - 1); // 去除最后一个&
//
//            // 3. SHA1加密
//            MessageDigest md = MessageDigest.getInstance("SHA-1");
//            md.update(string1.getBytes());
//            byte[] digest = md.digest();
//
//            // 4. 转为十六进制字符串
//            StringBuilder hexStr = new StringBuilder();
//            for (byte b : digest) {
//                String hex = Integer.toHexString(b & 0xFF);
//                if (hex.length() == 1) {
//                    hexStr.append("0");
//                }
//                hexStr.append(hex);
//            }
//            return hexStr.toString();
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException("生成微信签名失败", e);
//        }
//    }
//}