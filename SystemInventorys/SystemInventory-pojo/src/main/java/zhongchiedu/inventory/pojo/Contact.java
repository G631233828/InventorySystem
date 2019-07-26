package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contact {
	
	private String contactName;//联系人姓名
	
	private String contactNumber;//联系人电话
	
	private String qq;//联系人qq
	
	private String wechat;//联系人微信
	
	private String email;//联系人email

}
