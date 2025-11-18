package zhongchiedu.wechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import me.chanjar.weixin.mp.api.WxMpService;

@Controller
@RequestMapping("/wechat")
public class RepairController {

	@Autowired
	private WxMpService wxMpService;



	@Value("${wx.mp.configs[0].appId}")
	private String appid;
	@Value("${wx.mp.configs[0].secret}")
	private String secret;


	/**
	 * 跳转到添加页面
	 */
	@GetMapping(value = "/repair")
	public String addPage(Model model) {
		return "school/repair";
	}


	
	
	
}
