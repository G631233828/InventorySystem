package zhongchiedu.inventory.pojo;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhongchiedu.common.utils.enums.RepairStatus;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.MultiMedia;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WxRepair extends GeneralBean<WxRepair> {
	
	private static final long serialVersionUID = -8703750605726195412L;
	
	
	private String openId;//微信openId
	private String workOrderNumber;//工单号
	
	private String reportClassroomRepair;//报修教室
	private String equipmentRepair;//报修设备
	private String faultInformation;//故障信息
	@DBRef
	private List<MultiMedia> photos;//故障设备照片
	private String urgencyLevel;//紧急程度
	private String expectedVisitTime;//期望上门时间
	private Integer status;//维修状态  1.待处理 2.已分配 3.处理中 4.已完成 5.已取消
	@DBRef
	private WxReporter wxReporter; //报修人信息保存
	
	@DBRef
	private WxBinding worker;//绑定维修人员
	
	
	@DBRef
	private List<MultiMedia> repairPhotos;//完成维修照片
	
	private String repairContent;//维修内容
	
	private String completeTime;//维修完成时间
	
	private String workDept;//工单状态  销售部  工程部
	
	private String equipmentYear;//设备年份
	@DBRef
	private AfterSalesProjects project;
	
	
	
	
	

}
