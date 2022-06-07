package zhongchiedu.wx.service;

import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.ProjectPickup;
import zhongchiedu.wx.pojo.WXUserInfo;

public interface WXUserInfoService  extends GeneralService<WXUserInfo>{

	
	WXUserInfo saveOrUpdate(WXUserInfo wXUserInfo);
	 
	 WXUserInfo findUserByOpenId(String openId);
	 
	 WXUserInfo updateWXuserInfo(ProjectPickup p);
}
