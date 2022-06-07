package zhongchiedu.wx.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.common.utils.Types.menuType;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class WeChatMenu  extends GeneralBean<WeChatMenu>{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7523048561876555130L;
	
	
	private String name; //菜单名称
	private menuType type;//菜单类型
	private String url;  //菜单跳转地址 
	private String key;  //菜单key
	private String parentId;// 0为父id 
	private List<WeChatMenu> list;
	
	
	
	
	
	
}
