package zhongchiedu.wx.service;

import java.util.List;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.wx.pojo.Menu;
import zhongchiedu.wx.pojo.WeChatMenu;

public interface WeChatMenuService  extends GeneralService<WeChatMenu>{

	
	 List<WeChatMenu> findWeChatMenus();
	
	 void saveOrUpdate(WeChatMenu weChatMenu);
	
	 int getParentSize(String parentId);
	 
	 BasicDataResult findWeChatMenuByid(String id);
	
	 void deleteWeChatMenuById(String id);
	
	 BasicDataResult release();
	 
	 Menu getMenu();
	
}
