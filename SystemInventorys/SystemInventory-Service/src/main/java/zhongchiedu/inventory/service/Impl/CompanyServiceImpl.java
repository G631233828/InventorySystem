package zhongchiedu.inventory.service.Impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.service.CompanyService;

@Service
public class CompanyServiceImpl extends GeneralServiceImpl<Companys> implements CompanyService {

	@Override
	public void saveOrUpdate(Companys company) {
		if (Common.isNotEmpty(company)) {
			if (Common.isNotEmpty(company.getId())) {
				// update
				Companys ed = this.findOneById(company.getId(), Companys.class);
				BeanUtils.copyProperties(company, ed);
				this.save(company);
			} else {
				// insert
				this.insert(company);
			}
		}
	}

	@Override
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Companys company = this.findOneById(id, Companys.class);
		if (Common.isEmpty(company)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		company.setIsDisable(company.getIsDisable().equals(true) ? false : true);
		this.save(company);
		return BasicDataResult.build(200, company.getIsDisable().equals(true) ? "禁用成功" : "恢复成功",
				company.getIsDisable());
	}

	@Override
	public List<Companys> findAllCompany(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Companys.class);
	}

	@Override
	public String delete(String id) {
		try {
			List<String> ids = Arrays.asList(id.split(","));
//			List<Update> listUp = new ArrayList<Update>();
//			
//			Update update = null;
//			for (String edid : ids) {
//				listUp.add(Update.update("id", edid).set("isDelete", true));
//			}
			
		Update update = Update.update("_id", new ObjectId("5cd53b10a60d65d40c8fb65f")).set("isDelete", true);
			
			
			this.updateAllByQuery(new Query(),update, Companys.class);
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "error";
	}

}
