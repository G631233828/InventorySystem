package zhongchiedu.inventory.pojo;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.MultiMedia;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WxRepair extends GeneralBean<WxRepair> {
	
	private static final long serialVersionUID = -8703750605726195412L;
	
	private String schoolName;//学校名称
	private String schoolAddress;//学校地址 支持微信定位直接显示地址
	private String campus;//校区
	private String userName;//报修人
	private String contactNumber;//报修人联系电话
	private String reportClassroomRepair;//报修教室
	private String equipmentRepair;//报修设备
	private String faultInformation;//故障信息
	private String urgencyLevel;//紧急程度
	private String expectedVisitTime;//期望上门时间
	@DBRef
	private List<MultiMedia> photos;//故障设备照片

}
