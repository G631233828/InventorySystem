package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

/**
 * 客户
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NewCustomer extends GeneralBean<NewCustomer> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7036888586992963311L;
	
	private String name;
}
