package zhongchiedu.inventory.pojo;

import org.springframework.data.annotation.Transient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

/**
 * 常见报修项（设备名称+故障描述）
 * @author gjb
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommonRepairItem extends GeneralBean<CommonRepairItem> {
	
	private static final long serialVersionUID = 1234567890123456789L;
	
	/** 设备名称（如：打印机、电脑、水龙头） */
	private String deviceName;
	
	/** 故障描述（如：卡纸、蓝屏、漏水） */
	private String faultDesc;
	
}