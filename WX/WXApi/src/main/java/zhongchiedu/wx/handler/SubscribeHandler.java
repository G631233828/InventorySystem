package zhongchiedu.wx.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import zhongchiedu.wx.builder.TextBuilder;

/**
 * @author Binary Wang(https://github.com/binarywang)
 */
@Component
public class SubscribeHandler extends AbstractHandler {
	
	@Override
	public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService weixinService,
			WxSessionManager sessionManager) throws WxErrorException {

		this.logger.info("新关注用户 OPENID: " + wxMessage.getFromUser());

		// 获取微信用户基本信息
		try {
			WxMpUser userWxInfo = weixinService.getUserService().userInfo(wxMessage.getFromUser(), null);
			if (userWxInfo != null) {
				// TODO 可以添加关注用户到本地数据库
			}
		} catch (WxErrorException e) {
			if (e.getError().getErrorCode() == 48001) {
				this.logger.info("该公众号没有获取用户信息权限！");
			}
		}

		WxMpXmlOutMessage responseResult = null;
		try {
			responseResult = this.handleSpecial(wxMessage);
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

		if (responseResult != null) {
			return responseResult;
		}

		try {
			// {"mediaId":"5nr1LXNfLU7EU0E_OOPeCh24WANc4qFxQtapxrPXBbE","url":"http://mmbiz.qpic.cn/mmbiz_png/r6gKMSiab3lj4MSYPTRlFn1USpsKkulNiaINLQsxCxTanN9fEpZgrPPq0ice2ia2zckzpwLRHSEYuztocc4V1jXDNA/0?wx_fmt=png"}
			//WeChatSetting wechatSetting = new WeChatSetting();
			
			return new TextBuilder().build("感谢关注！", wxMessage, weixinService);

		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * 处理特殊请求，比如如果是扫码进来的，可以做相应处理
	 */
	private WxMpXmlOutMessage handleSpecial(WxMpXmlMessage wxMessage) throws Exception {
		// TODO
		return null;
	}

}
