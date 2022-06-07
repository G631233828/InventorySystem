package zhongchiedu.general.service;

import java.util.List;

import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.general.pojo.User;

public interface UserService  extends GeneralService<User>{

	public User findUserByOpenId(String openId);
	
	public User findUserByUserNameAndPassword(String username,String password);
	
	public List<User> findAllUser();
	
	public void updateUserAddOpenId(String userId,String openId);

	public User findUserById(String id);

	public User findUserByAccountName(String username);
	
	public List<User> findUserInIds(List<Object> id);
	
	public List<User> getUsersByIds(String personInCharge);
	
}
