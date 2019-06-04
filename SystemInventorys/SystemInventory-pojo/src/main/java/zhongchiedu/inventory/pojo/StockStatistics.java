package zhongchiedu.inventory.pojo;

import java.util.Date;

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
public class StockStatistics extends GeneralBean<StockStatistics> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8545694298499828935L;
	
	
	@DBRef
	private Stock stock; //绑定库存商品
	private Date storageTime;//入库时间
	private Date depotTime;//出库时间
	private long num;//出库、入库数量
	private long newNum;//当前库存
	@DBRef
	private Companys company;//绑定企业
	@DBRef
	private User user;//操作人
	private boolean revoke; //是否可撤销
	private boolean InOrOut;//入库还是出库
	private long revokeNum;//撤销数量，默认为全部，撤销数量不能大于入库或者出库数量
	

	


	
	

}
