package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 品牌管理
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Brand extends GeneralBean<Brand> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3667587084298879810L;
	/**
	 * 
	 */
	private String name;

}
