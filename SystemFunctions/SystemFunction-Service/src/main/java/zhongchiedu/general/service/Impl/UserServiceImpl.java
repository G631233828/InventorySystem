package zhongchiedu.general.service.Impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.pojo.Resource;
import zhongchiedu.general.pojo.Role;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.UserService;

@Service
public class UserServiceImpl extends GeneralServiceImpl<User> implements UserService {


	@Autowired
	private RoleServiceImpl roleService;
	
	@Autowired
	private MultiMediaServiceImpl multiMediaSerice;

	/**
	 * 用户添加或修改
	 * 
	 * @param user
	 * @param roleId
	 * @return
	 */
	public void saveOrUpdateUser(User user, String roleId ,MultipartFile[] file ,String imgPath,String dir,String oldheadImg) {
		User getUser = null;
		List<MultiMedia> userHead = null; 
			userHead = this.multiMediaSerice.uploadPictures(file, dir, imgPath, "USER",105,105);
	
		if(Common.isNotEmpty(user.getId())){
			getUser = this.findOneById(user.getId(), User.class);
			if(Common.isNotEmpty(getUser)){
				user.setPhotograph(Common.isNotEmpty(oldheadImg)?getUser.getPhotograph():null);
				user.setAccountName(getUser.getAccountName());
			}
		}
		if(userHead.size()>0){
			user.setPhotograph(userHead.get(0));
		}
		Role role = this.roleService.findRoleById(roleId);
		user.setRole(role != null ? role : null);
		if(Common.isNotEmpty(getUser)){
			BeanUtils.copyProperties(user, getUser);
			this.save(user);
		}else{
			this.insert(user);
		}
		
	}
//	public void saveOrUpdateUser(User user, String roleId ) {
//		
//		if (Common.isNotEmpty(user)) {
//			
//			if (Common.isNotEmpty(user.getId())) {
//				// 执行修改操作
//				User eduser = this.findUserById(user.getId());
//				
//				if (eduser == null)
//					eduser = new User();
//				eduser.setCardId(user.getCardId());
//				eduser.setCardType(user.getCardType());
//				eduser.setPhotograph(user.getPhotograph());
//				eduser.setUserName(user.getUserName());
//				eduser.setPassWord(user.getPassWord());
////				eduser.setSchool(school);
//				//eduser.setPhotograph(user.getPhotograph());
//				Role role = this.roleService.findRoleById(roleId);
//				// 通过hid获取角色信息，如果为null 返回null否则返回role
//				eduser.setRole(role != null ? role : null);
//				this.save(eduser);
//			} else {
//				User aduser = new User();
//				aduser.setAccountName(user.getAccountName());
//				aduser.setCardId(user.getCardId());
//				aduser.setCardType(user.getCardType());
//				aduser.setIsDisable(false);
//				// aduser.setPhotograph(user.getPhotograph());
//				aduser.setUserName(user.getUserName());
//				aduser.setPassWord(user.getPassWord());
////				aduser.setSchool(school);
//				// 默认分配一个空的集合
//				aduser.setResource(new ArrayList<Resource>());
//				Role role = this.roleService.findRoleById(roleId);
//				aduser.setRole(role != null ? role : null);
//				// 执行添加操作
//				this.insert(aduser);
//			}
//		}
//		
//	}

	/**
	 * 根据用户帐号查询用户信息
	 * 
	 * @param accountName
	 * @return
	 * @throws Exception
	 */
	public User findUserByAccountName(String accountName) {
		Query query = new Query();
		query.addCriteria(Criteria.where("accountName").is(accountName));
		User user = this.findOneByQuery(query, User.class);
		
		return user!=null?user:null;
	}

	/**
	 * 根据用户id查询用户
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public User findUserById(String id) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(id));
		// User user= this.userDao.findOneById(id);
		User user = this.findOneByQuery(query, User.class);
		return user!=null?user:null;
		
	}

	public List<User> findAllUser() throws Exception {
		List<User> listuser = this.find(new Query(), User.class);
		if (listuser != null)
			return listuser;
		else
			return null;
	}

	
	//查询重复帐号
	public BasicDataResult ajaxgetRepletes(String accountName) {
		if(Common.isNotEmpty(accountName)){
			User user = this.findUserByAccountName(accountName);
			
			if(user!=null){
				return BasicDataResult.build(206,"当前账号已经被注册，请重新输入", null);
			}
			
			return BasicDataResult.ok();
		}
		
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
		
	}

	public BasicDataResult userDisable(String id) {
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		User user = this.findUserById(id);
		if(user == null){
			return BasicDataResult.build(400, "无法获取到用户信息，该用户可能已经被删除", null);
		}
		
		user.setIsDisable(user.getIsDisable().equals(true)?false:true);
		this.save(user);
		
		return BasicDataResult.build(200, user.getIsDisable().equals(true)?"用户禁用成功":"用户启用成功",user.getIsDisable());
		
	}
	
	
	public BasicDataResult checkPassword(String id,String password){
		
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "发生未知异常，请联系管理员！", null);
		}
		if(Common.isEmpty(password)){
			return BasicDataResult.build(400, "请输入旧密码", null);
		}
		User user = this.findOneById(id, User.class);
		if(Common.isEmpty(user)){
			return BasicDataResult.build(400, "未能获取到数据，请刷新后再试", null);
		}
		if(user.getPassWord().equals(password)){
			return BasicDataResult.build(200, "密码正确", null);
		}
		return BasicDataResult.build(400, "发生未知异常，请联系管理员！", null);
		
	}
	
	public BasicDataResult editPassword(String id,String password){
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "发生未知异常，请联系管理员！", null);
		}
		if(Common.isEmpty(password)){
			return BasicDataResult.build(400, "新密码不能为空", null);
		}
		User user = this.findOneById(id, User.class);
		if(Common.isEmpty(user)){
			return BasicDataResult.build(400, "未能获取到数据，请刷新后再试", null);
		}
		user.setPassWord(password);
		this.save(user);
		return BasicDataResult.build(200, "密码修改成功", null);
		
	}
	

}
