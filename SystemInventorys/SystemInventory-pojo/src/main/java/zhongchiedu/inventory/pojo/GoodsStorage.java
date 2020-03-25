package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GoodsStorage extends GeneralBean<GoodsStorage> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6757367985024169272L;
	/**
	 * 
	 */
	private String address;
	private String shelfNumber;
	private String shelflevel;
	@DBRef
	private Companys companys;
	@DBRef
	private Area area;

}
