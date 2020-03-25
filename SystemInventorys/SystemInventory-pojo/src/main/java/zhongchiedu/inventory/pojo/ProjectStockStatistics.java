package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;


/**
 * 库存统计
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProjectStockStatistics extends GeneralBean<ProjectStockStatistics> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8545694298499828935L;
	
	
	@DBRef
	private ProjectStock projectStock; //绑定库存商品
	private String storageTime;//入库时间
	private String depotTime;//出库时间
	private Integer num;//出库、入库数量
	private Integer newNum;//当前库存
	private Integer actualPurchaseQuantity;//实际采购量 
	@DBRef
	private Companys company;//绑定企业
	@DBRef
	private User user;//操作人
	private boolean revoke; //是否可撤销
	private boolean inOrOut;//入库还是出库
	private Integer revokeNum;//撤销数量，默认为全部，撤销数量不能大于入库或者出库数量
	
	private String personInCharge;//责任人
	private String projectName;   //项目名
	private String customer;//客户
	@DBRef
	private Area area;

	


	
	

}
