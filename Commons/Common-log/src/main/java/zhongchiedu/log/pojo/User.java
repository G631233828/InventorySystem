package zhongchiedu.log.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends GeneralBean<User> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5419852964729823329L;
	private String userName;		//用户姓名
	private String cardType;		//用户证件类型
	private String cardId;			//证件号码
	private String accountName; 	//登录账户
	private String passWord;		//登录密码
	private String lastLoginTime;   //登录时间
	private String lastLoginIp;		//上次登录Ip
	private String photograph;		//用户头像
	private String salt;//加密密码的盐
	
	


	
}
	
