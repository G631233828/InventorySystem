package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

/*
 * 	 施工队出库记录
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProjectPickup extends GeneralBean<ProjectPickup> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4195297404117883960L;

	private String openId;
	private String nickName;// 取货人微信
	private String name;// 取货设备
	private String model;// 取货设备型号
	private String username;// 取货人姓名
	private String phone;// 取货人手机号
	private Integer num;// 取货数量
	
	private String entryName;// 项目名称
	private String itemNo;// 项目编号
	private String projectLeader;// 项目负责人
	
	private String sign;// 微信签名
	
	private boolean status; //false 未出库  true 已经出库
	
	@DBRef
	private Stock stock;
}
