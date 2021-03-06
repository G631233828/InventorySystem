package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 单位管理
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Unit extends GeneralBean<Unit> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2850255059042387892L;
	/**
	 * 
	 */
	private String name;

}
