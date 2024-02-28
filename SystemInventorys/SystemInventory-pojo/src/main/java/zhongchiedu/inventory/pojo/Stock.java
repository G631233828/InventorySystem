package zhongchiedu.inventory.pojo;

import java.math.BigDecimal;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.pojo.User;

/**
 * 库存管理管理
 * 
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
	private String name;// 设备名称
	private String model;// 设备型号
	private String scope;// 使用范围
	@DBRef
	private GoodsStorage goodsStorage;// 货架号/层

	@DBRef
	private SystemClassification systemClassification;//系统分类

	private String price;// 单价
	@DBRef
	private Unit unit; // 计量单位
	private String maintenance;// 维保
	@DBRef
	private Supplier supplier;// 供应商
	private long inventory = 0; // 库存量
	
	@Transient
	private long remainingNum;//剩余库存
	

	private boolean receivables = false;// 项目应收款
	@DBRef
	private Area area;

	private String type;// 入库方式 1.电脑 2手机

	private String barCode;// 条形码
	
	@DBRef
	private User publisher;//发布人
	
	@DBRef
	private QrCode qrCode;//库存二维码
	
	@DBRef
	private Brand brand;//品牌管理
	
	
	
	
	//new
	private String entryName;//项目名称
	private String itemNo;//项目编号
	
	//添加代理商品标记
	private  boolean agent;//是否代理商品 false 非代理商品  true代理商品
	@Transient 
	private long stocknum;//导入库存数量 不会将这个数据放入数据库
	
	
	


}
