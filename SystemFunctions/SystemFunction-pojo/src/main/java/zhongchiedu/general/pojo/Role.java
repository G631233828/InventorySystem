package zhongchiedu.general.pojo;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role extends GeneralBean<Role> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6971871578588624227L;
	private String roleName; // 角色名称
	private String roleKey; // 角色key
	@DBRef
	private List<Resource> resource;

	private int version; //当前版本
	
	
	
}
