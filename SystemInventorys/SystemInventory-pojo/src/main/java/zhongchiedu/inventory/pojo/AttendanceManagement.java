package zhongchiedu.inventory.pojo;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.MultiMedia;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AttendanceManagement extends GeneralBean<AttendanceManagement> {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;//姓名
	@DBRef
	private School school;//
	
	private String address;//定位

	@DBRef
	private List<MultiMedia> photos;//签到照片
	
	private String problems;//现场问题反馈
	
	private String openId;
	
	private String signTime;
	
	private String classRoom;//签到教室
	
	

}
