package zhongchiedu.log.pojo;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;


@Document
@Data
public class Log extends GeneralBean<Log>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -574653567328003244L;
	
	private String method;
	private String type;
	private String requestIp;
	private String exceptionCode;
	private String exceptionDetail;
	private String params;
	private String createby;
	private String userId;
	@DBRef  
	private User user;
	@DBRef
	private List<Log> logs;
	
	
	
	

}
