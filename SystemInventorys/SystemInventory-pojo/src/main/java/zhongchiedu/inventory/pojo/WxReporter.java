package zhongchiedu.inventory.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WxReporter extends GeneralBean<WxReporter> {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String openId;//微信openId
	private String schoolName;//学校名称
	private String schoolAddress;//学校地址 支持微信定位直接显示地址
	private String campus;//校区
	private String userName;//报修人
	private String contactNumber;//报修人联系电话
	

}
