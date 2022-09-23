package zhongchiedu.inventory.pojo;

import java.util.Date;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;

/***
 * 
 * @author gjb
 * 库存中转 用来记录未入库直接给到客户的设备
 * 
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryTransfer extends GeneralBean<InventoryTransfer>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2644417343883517523L;
	
	private String name;// 设备名称
	private String model;// 设备型号
	private String scope;// 使用范围
	
	private String price;// 单价
	private long  transferQuantity; // 库存中转数量
	
	private String inventoryTransferDate;//库存流转日期
	
	
	@DBRef
	private Unit unit; // 计量单位
	private String maintenance;// 维保
	@DBRef
	private Supplier supplier;// 供应商

	private boolean receivables = false;// 项目应收款

	private String personInCharge;//负责人
	
	//new
	private String entryName;//项目名称
	private String itemNo;//项目编号
	private String customer;//客户
	private String contactNumber;//联系电话
	

	
	
	
	

}
