package zhongchiedu.inventory.pojo;

import org.springframework.data.annotation.Transient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 售后项目保障
 * @author gjb
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AfterSalesProjects  extends GeneralBean<AfterSalesProjects>{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8671281908672978029L;
	
	private String projectName;//项目名称
	private String schoolName;//学校名称
	private String winningBidder;//中标单位
	private String contactTeacher;//联系老师
	private String teacherPhone;  //老师电话
	private String schoolAddress;//学校地址
	private String worker;//施工队
	private String warrantyStartTime;//保修开始时间
	private String warrantyEndTime;//保修截止时间
	private String projectYear;//项目年份
	
	

}
