package zhongchiedu.wechat.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.Tess4jUtil;

@Controller
@RequestMapping("/wechat")
public class MediaController {

	@Autowired
	private WxMpService wxMpService;
	

	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;

	@RequestMapping("/getMedia")
	@ResponseBody
	public BasicDataResult getMedia(String mediaId) {
		System.out.println(mediaId);
		String take ="";
		try {
			File mediaDownload = this.wxMpService.getMaterialService().mediaDownload(mediaId);
			 take = Tess4jUtil.take(mediaDownload.getAbsolutePath(), "C:\\Users\\fliay\\Downloads\\tessdata-master\\tessdata-master",Contents.CHI_SIM);
			System.out.println(take);
		} catch (WxErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new BasicDataResult().build(200, "分析完成", take);
	}
}
