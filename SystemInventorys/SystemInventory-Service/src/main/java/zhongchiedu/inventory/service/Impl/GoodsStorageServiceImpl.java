package zhongchiedu.inventory.service.Impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.Resource;
import zhongchiedu.inventory.pojo.Companys;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.service.GoodsStorageService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class GoodsStorageServiceImpl extends GeneralServiceImpl<GoodsStorage> implements GoodsStorageService {

	@Override
	@SystemServiceLog(description="编辑货架信息")
	public void saveOrUpdate(GoodsStorage goodsStorage) {
		if (Common.isNotEmpty(goodsStorage)) {
			if (Common.isNotEmpty(goodsStorage.getId())) {
				// update
				GoodsStorage ed = this.findOneById(goodsStorage.getId(), GoodsStorage.class);
				BeanUtils.copyProperties(goodsStorage, ed);
				this.save(goodsStorage);
			} else {
				// insert
				this.insert(goodsStorage);
			}
		}
	}

	@Override
	@SystemServiceLog(description="启用禁用货架信息")
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		GoodsStorage goodsStorage = this.findOneById(id, GoodsStorage.class);
		if (Common.isEmpty(goodsStorage)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		goodsStorage.setIsDisable(goodsStorage.getIsDisable().equals(true) ? false : true);
		this.save(goodsStorage);
		return BasicDataResult.build(200, goodsStorage.getIsDisable().equals(true) ? "禁用成功" : "恢复成功",
				goodsStorage.getIsDisable());
	}

	@Override
	@SystemServiceLog(description="获取所有非禁用货架信息")
	public List<GoodsStorage> findAllGoodsStorage(boolean isdisable,String areaId) {
		Query query = new Query();
		if(Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, GoodsStorage.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description="删除货架信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				GoodsStorage de = this.findOneById(edid, GoodsStorage.class);
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
	@SystemServiceLog(description="分页查询货架信息")
	public Pagination<GoodsStorage> findpagination(Integer pageNo, Integer pageSize,String searchArea) {
		// 分页查询数据
		Pagination<GoodsStorage> pagination = null;
		try {
			Query query = new Query();
			if(Common.isNotEmpty(searchArea)) {
				query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			}
			query.addCriteria(Criteria.where("isDelete").is(false));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, GoodsStorage.class);
			if (pagination == null)
				pagination = new Pagination<GoodsStorage>();
			return pagination;
		} catch (Exception e) {
			log.info("查询所有货架信息失败——————————》" + e.toString());
			e.printStackTrace();
		}
		return pagination;
	}

	@Override
	@SystemServiceLog(description="查询是否有重复货架信息")
	public BasicDataResult ajaxgetRepletes(String address, String shelfNumber, String shelflevel) {
		if (Common.isNotEmpty(address)&&Common.isNotEmpty(shelfNumber)&&Common.isNotEmpty(shelflevel)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("address").is(address)).addCriteria(Criteria.where("shelfNumber").is(shelfNumber)).addCriteria(Criteria.where("shelflevel").is(shelflevel));
			query.addCriteria(Criteria.where("isDelete").is(false));
			GoodsStorage goodsStorage = this.findOneByQuery(query, GoodsStorage.class);
			 return goodsStorage != null ?BasicDataResult.build(206,"当前货架信息已经存在，请检查", null): BasicDataResult.ok();
		}
		return BasicDataResult.build(400,"未能获取到请求的信息", null);
	}

	@Override
	@SystemServiceLog(description="启用禁用货架信息")
	public BasicDataResult todisable(String id) {
		
		if(Common.isEmpty(id)){
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		GoodsStorage goodsStorage = this.findOneById(id, GoodsStorage.class);
		if(goodsStorage == null){
			return BasicDataResult.build(400, "无法获取到企业信息，该用户可能已经被删除", null);
		}
		goodsStorage.setIsDisable(goodsStorage.getIsDisable().equals(true)?false:true);
		this.save(goodsStorage);
		
		return BasicDataResult.build(200, goodsStorage.getIsDisable().equals(true)?"禁用成功":"启用成功",goodsStorage.getIsDisable());
		
	}

	public List<GoodsStorage> findStorages(String areaId) {
		Query query= new Query();
		query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		query.addCriteria(Criteria.where("isDisable").is(false));
		List<GoodsStorage> list = this.find(query,GoodsStorage.class);
		return list.size()>0?list:null;
	}


	
	
	
	
	
	

}


















