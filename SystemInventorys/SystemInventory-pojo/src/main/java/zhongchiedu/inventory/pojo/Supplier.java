package zhongchiedu.inventory.pojo;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 供应商管理
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Supplier extends GeneralBean<Supplier> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3720220156014158163L;
	/**
	 * 
	 */
	private String name;//供应商名称
	@DBRef
	private SystemClassification systemClassification;//系统分类
	@DBRef
	private List<Category> categorys;  //目录列表
	@DBRef
	private Brand brand;//品牌
	private String contact;//联系人
	private String contactNumber;//联系电话
	private String qq;
	private String wechat;
	private String email;
	private String address;
	private String introducer;//介绍人
	private String productQuality;//产品质量
	private String price;//价格
	private String supplyPeriod;//供货期
	private String payMent;//付款条件
	private String afterSaleService;//售后服务
	
	
	
	
	

}
