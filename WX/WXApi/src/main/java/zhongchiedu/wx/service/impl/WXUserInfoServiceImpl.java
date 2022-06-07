package zhongchiedu.wx.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.ProjectPickup;
import zhongchiedu.wx.pojo.WXUserInfo;
import zhongchiedu.wx.service.WXUserInfoService;
@Service
public class WXUserInfoServiceImpl extends GeneralServiceImpl<WXUserInfo> implements WXUserInfoService {

	@Override
	public WXUserInfo saveOrUpdate(WXUserInfo wXUserInfo) {
		if (Common.isNotEmpty(wXUserInfo)) {
			if (Common.isNotEmpty(wXUserInfo.getId())) {
				WXUserInfo ed = this.findOneById(wXUserInfo.getId(), WXUserInfo.class);
				BeanUtils.copyProperties(wXUserInfo, ed);
				this.save(wXUserInfo);
				return wXUserInfo;
			} else {
					WXUserInfo getwx = this.findUserByOpenId(wXUserInfo.getOpenId());
				if(Common.isEmpty(getwx)) {
					this.insert(wXUserInfo);
					return wXUserInfo;
				}else {
					return getwx;
				}
			}
			
		}
		return null;

	}

	@Override
	public WXUserInfo findUserByOpenId(String openId) {
		if (Common.isNotEmpty(openId)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("openId").is(openId));
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.addCriteria(Criteria.where("isDisable").is(false));
			return this.findOneByQuery(query, WXUserInfo.class);
		}
		return null;

	}

	@Override
	public WXUserInfo updateWXuserInfo(ProjectPickup p) {
		WXUserInfo wXUserInfo = this.findUserByOpenId(p.getOpenId());
		wXUserInfo.setUsername(p.getUsername());
		wXUserInfo.setPhone(p.getPhone());
		wXUserInfo.setOpenId(p.getOpenId());
		this.save(wXUserInfo);
		return wXUserInfo;
	}
	
	
	
	
	
	
	
	
	
	
	

}
