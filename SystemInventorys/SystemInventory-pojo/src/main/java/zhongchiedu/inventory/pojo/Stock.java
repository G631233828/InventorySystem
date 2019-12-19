package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 库存管理管理
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Stock extends GeneralBean<Stock> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8703750605726195412L;
	private String name;//设备名称
	private String model;//设备型号
	private String scope;//使用范围
	@DBRef
	private GoodsStorage goodsStorage;//货架号/层
	private String price;//单价
	@DBRef
	private Unit unit; //计量单位
	private String maintenance;//维保
	@DBRef
	private Supplier supplier;//供应商
	private long inventory = 0; //库存量
	
	private boolean receivables = false;//项目应收款
	
	
	
	

}
