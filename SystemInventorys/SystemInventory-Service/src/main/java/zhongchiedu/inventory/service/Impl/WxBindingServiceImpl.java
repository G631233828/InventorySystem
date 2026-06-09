package zhongchiedu.inventory.service.Impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.enums.PersonJoinAuditStatusEnum;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.Impl.MultiMediaServiceImpl;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;

@Service
@Slf4j
public class WxBindingServiceImpl extends GeneralServiceImpl<WxBinding> implements WxBindingService {

	@Override
	public Pagination<WxBinding> findpagination(Integer pageNo, Integer pageSize) {
		// 分页查询数据
		Pagination<WxBinding> pagination = null;
		try {
			Query query = new Query();

			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, WxBinding.class);
			if (pagination == null)
				pagination = new Pagination<WxBinding>();
			return pagination;
		} catch (Exception e) {
			log.info("查询所有绑定信息——————————》" + e.toString());
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	public void saveOrUpdate(WxBinding wxBinding) {

		if (Common.isNotEmpty(wxBinding)) {
			if (Common.isNotEmpty(wxBinding.getId())) {
				// update
				WxBinding ed = this.findOneById(wxBinding.getId(), WxBinding.class);
				BeanUtils.copyProperties(wxBinding, ed);
				this.save(wxBinding);
			} else {
				wxBinding.setAuditStatus(PersonJoinAuditStatusEnum.SUBMITTED.getCode());
				// insert
				this.insert(wxBinding);
			}
		}
	}

	private Lock lock = new ReentrantLock();

	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				WxBinding de = this.findOneById(edid, WxBinding.class);
				de.setIsDelete(true);
				this.save(de);
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
	public WxBinding findWxBindingByOpenId(String openId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
//		query.addCriteria(Criteria.where("auditStatus").is(PersonJoinAuditStatusEnum.APPROVED));//提交成功状态
		query.addCriteria(Criteria.where("openId").is(openId));
		return this.findOneByQuery(query, WxBinding.class);
	}

	@Override
	public List<WxBinding> findBindingsByPersonnelType(PersonnelType p,PersonJoinAuditStatusEnum a) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		query.addCriteria(Criteria.where("auditStatus").is(a.getCode()));
		query.addCriteria(Criteria.where("PersonnelType").is(p.getCode()));
		return this.find(query, WxBinding.class);
	}

	@Override
	public boolean auditWxBinding(String id, Integer status) {
	    WxBinding wxBinding = this.findOneById(id, WxBinding.class);

	    if (wxBinding == null) {
	        throw new RuntimeException("微信绑定记录不存在！");
	    }

	    // 新增：判断是否重复审核（当前状态与目标状态一致）
	    Integer currentStatus = wxBinding.getAuditStatus();
	    if (currentStatus != null && currentStatus.equals(status)) {
	        String statusDesc = status == 2 ? "审核通过" : "审核拒绝";
	        throw new RuntimeException("该记录当前已是【" + statusDesc + "】状态，无需重复操作！");
	    }

	    // 校验是否已审核（可选：如果已审核且状态不同，也可阻止）
	    if (currentStatus != null && currentStatus == status) {
	        throw new RuntimeException("该记录已审核（当前状态：" + 
	                (currentStatus == 2 ? "通过" : currentStatus == 3 ? "拒绝" : "未知") + "），无法重复审核！");
	    }

	    // 更新审核状态
	    wxBinding.setAuditStatus(status);
	    this.save(wxBinding);
	    return true;
	}

    @Override
    public WxBinding findByName(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        query.addCriteria(Criteria.where("isDelete").is(false));
        query.addCriteria(Criteria.where("auditStatus").is(2));
        return this.findOneByQuery(query, WxBinding.class);
    }

}
