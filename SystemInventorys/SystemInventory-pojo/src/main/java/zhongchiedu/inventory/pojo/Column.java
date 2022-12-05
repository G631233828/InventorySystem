package zhongchiedu.inventory.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

/**
 * 显示列
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Column extends GeneralBean<Column> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;//集合名称
	private List<String> columns;//显示的列
	private String userId;
	
	
}
