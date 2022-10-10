package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

/***
 * 
 * @author gjb
 * 维修管理
 * 
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RepairManagement extends GeneralBean<RepairManagement>{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8571456818967739286L;
	private String name;// 设备名称
	private int repairNum;//维修数量
	private String pickingDate;//取件日期
	private String picker;//取件人
	private String source;//来源
	private boolean status;//维修状态
	//维修完成
	private String sender;//寄件人
	private String dateOfSending;//寄件日期
	private String mailingOrderNo;//寄件单号
	private String address;//收货地址
	private String recipientName;//收件人姓名
	private String recipientTel;//收件人电话
	private int quantityResurned;//返还数量
	private String quantityDate;//返还日期
	private String returnProcessing;//返货处理
	
	
	
	

}
