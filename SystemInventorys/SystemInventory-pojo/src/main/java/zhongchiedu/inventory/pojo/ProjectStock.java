package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 项目库存管理
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProjectStock extends GeneralBean<ProjectStock> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8703750605726195412L;
	
	private String projectName;//项目名称
	private String name;//设备名称
	private String model;//设备型号
	private String scope;//使用范围
	
	private long projectedProcurementVolume=0;//预计采购量#
	private long estimatedUnitPrice=0;//预计采购单价
	private long projectedTotalPurchasePrice=0;//预计采购总价
	
	@DBRef
	private Supplier supplier;//供应商
	
	private long actualPurchaseQuantity=0;//实际采购数量  #
	private long realCostUnitPrice=0;//实际成本单价
	private long totalActualCost=0;//实际成本总价
	
	private String paymentTime;//付款时间
	private long paymentAmount=0;//付款金额
	
	private long num=0;//出库数量
	private long inventory = 0; //库存量

}
