package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class Companys extends GeneralBean<Companys> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4647607963249051690L;

	private String name;
	
	private String address;
	
	private String contact;
	
	
	
}
