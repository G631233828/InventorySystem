package zhongchiedu.inventory.pojo;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;


/**
 * 库存管理部分操作权限
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryRole extends GeneralBean<InventoryRole>{
	
	private String name;
	private String  type;
	@DBRef
	private List<User> users;//所有用户


}
