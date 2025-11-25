package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WxBinding extends GeneralBean<WxRepair>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6525546954688780939L;
	private String openId;        //微信openId
	private String team; 		  //所属施工队 
	private String name;          //姓名
	private String contactNumber; //联系电话
	private Integer personnelType;//用户类型   1.维修人员  2.调度人员
	

}
