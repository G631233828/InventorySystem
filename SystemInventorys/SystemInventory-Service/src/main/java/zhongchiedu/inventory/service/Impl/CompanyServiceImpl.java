package zhongchiedu.inventory.service.Impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.service.CompanyService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class CompanyServiceImpl extends GeneralServiceImpl<Companys> implements CompanyService {

	@Override
	@SystemServiceLog(description="编辑企业信息")
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
	@SystemServiceLog(description="启用禁用企业信息")
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
	@SystemServiceLog(description="获取所有非禁用企业信息")
	public List<Companys> findAllCompany(boolean isdisable) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, Companys.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description="删除企业信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Companys comp = this.findOneById(edid, Companys.class);
				comp.setIsDelete(true);
				this.save(comp);
			}
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return "error";
	}

	@Override
	@SystemServiceLog(description="分页查询企业信息")
	public Pagination<Companys> findpagination(Integer pageNo, Integer pageSize) {
		// 分页查询数据
		Pagination<Companys> pagination = null;
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Companys.class);
			if (pagination == null)
				pagination = new Pagination<Companys>();
			return pagination;
		} catch (Exception e) {
			log.info("查询所有企业信息失败——————————》" + e.toString());
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	@SystemServiceLog(description="根据名称查询是否存在企业信息")
	public BasicDataResult ajaxgetRepletes(String name) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			Companys companys = this.findOneByQuery(query, Companys.class);
			 return companys != null ?BasicDataResult.build(206,"当前企业已经存在，请检查", null): BasicDataResult.ok();
		}
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
	}

	@Override
	@SystemServiceLog(description="启用禁用企业信息")
	public BasicDataResult todisable(String id) {
		
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Companys companys = this.findOneById(id, Companys.class);
		if(companys == null){
			return BasicDataResult.build(400, "无法获取到企业信息，该用户可能已经被删除", null);
		}
		companys.setIsDisable(companys.getIsDisable().equals(true)?false:true);
		this.save(companys);
		
		return BasicDataResult.build(200, companys.getIsDisable().equals(true)?"企业禁用成功":"企业启用成功",companys.getIsDisable());
		
	}

}
