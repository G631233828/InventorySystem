package zhongchiedu.wx.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class WXUserInfo  extends GeneralBean<WXUserInfo>{/**
	 * 
	 */
	private static final long serialVersionUID = 7330368168703382173L;
	
	private WxOAuth2UserInfo userInfo;
	private String username;
	private String phone;
	private String openId;

	
}
