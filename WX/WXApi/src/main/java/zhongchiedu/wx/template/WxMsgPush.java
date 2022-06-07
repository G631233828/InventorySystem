package zhongchiedu.wx.template;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

@Component
@Slf4j
public class WxMsgPush {

	@Autowired
	private WxMpService wxMpService;
	

	
	public String sendWxMessage(String templateId,String openId,String url,Map<String,String> dataMap) {
		
		WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
				.toUser(openId)
				.templateId(templateId)
				.url(url)
				.build();
		//填充模板数据
		dataMap.forEach((k,v)->{
			templateMessage.addData(new WxMpTemplateData(k,v,""));
		});
		
		try {
			return this.wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
		} catch (WxErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "-1";
		}
	}
	
	
	
}
