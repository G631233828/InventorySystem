package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;

/**
 *  预入库
 * 
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PreStock extends GeneralBean<PreStock> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1453898869801540408L;
	private String name;// 设备名称
	private String model;// 设备型号
	private String scope;// 使用范围
	@DBRef
	private GoodsStorage goodsStorage;// 货架号/层
	private String price;// 单价
	@DBRef
	private Unit unit; // 计量单位
	private String maintenance;// 维保
	private boolean receivables = false;// 项目应收款
	
	@DBRef
	private Supplier supplier;// 供应商

	@DBRef
	private Area area;

	private long estimatedInventoryQuantity;// 预计库存数量

	private long actualReceiptQuantity;// 实际入库数量
	
//	private long receiptQuantity=0;//已入库数量
	
	@DBRef
	private User handler;// 处理人，负责人
	@DBRef
	private User publisher;//发布人
	
	private String estimatedWarehousingTime;//预计入库时间
	
	private int status =1;//状态    1.预入库状态  2.已入库 
	
	
	private String entryName;//项目名称
	private String itemNo;//项目编号

	//20230207  添加出库入库总金额
	private Double inprice;
	private String purchaseInvoiceNo;//采购发票号


}
