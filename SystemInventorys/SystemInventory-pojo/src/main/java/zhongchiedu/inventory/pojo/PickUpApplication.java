package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;

/**
 * 取货申请
 * 
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PickUpApplication extends GeneralBean<PickUpApplication> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2165416986092058320L;
	@DBRef
	private Area area;//库存地址
	@DBRef
	private Stock stock;//库存
	
	private String pickUpTime;//取货时间
	
	private String pickUpPerson;//取件人
	private String pickUpPhone;//取件人联系电话
	
	
	private String personInCharge;//负责人
	
	private String projectName;//项目名称
	
	private String customer;//客户
	
	private long estimatedIssueQuantity;// 预计出库数量

	private long actualIssueQuantity;// 实际出库数量
	
	private String num;//出库数量
	
	private int status =1;//状态    1.待出库状态  2.已出库 
	
	@DBRef
	private User handler;// 处理人，负责人
	@DBRef
	private User publisher;//发布人
	
	
	
}
