package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 类目管理
 * 用于添加系统的子分类等
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Category extends GeneralBean<Category> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1432846969158838995L;
	private String name;

}
