package zhongchiedu.inventory.pojo;

import java.util.List;

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
public class SystemClassification extends GeneralBean<SystemClassification> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 910155958381461814L;
	private String name;
	@DBRef
	private List<Category> categorys;

}
