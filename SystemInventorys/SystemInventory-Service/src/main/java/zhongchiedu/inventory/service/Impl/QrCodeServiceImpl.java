package zhongchiedu.inventory.service.Impl;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.MultiMediaService;
import zhongchiedu.inventory.pojo.QrCode;
import zhongchiedu.inventory.service.QrCodeService;


@Service
@Slf4j
public class QrCodeServiceImpl extends GeneralServiceImpl<QrCode> implements QrCodeService {

	@Autowired
	private MultiMediaService multiMediaService;

	@Override
	public Pagination<QrCode> findPagination(Integer pageNo, Integer pageSize) {
		Pagination<QrCode> pagination = null;
		Query query = new Query();

		query.addCriteria(Criteria.where("isDelete").is(false));
		try {
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, QrCode.class);

		} catch (Exception e) {
			log.info("查询所有信息失败——————————》" + e.toString());
			e.printStackTrace();
		}
		return Common.isNotEmpty(pagination) ? pagination : new Pagination<QrCode>();
	}

	@Override
	public void saveOrUpdate(QrCode qrCode) {
		QrCode ed = null;

		if (Common.isNotEmpty(qrCode.getId())) {
			ed = this.findOneById(qrCode.getId(), QrCode.class);
		}
		if (Common.isNotEmpty(ed)) {
			qrCode.setQrcode(ed.getQrcode());
			BeanUtils.copyProperties(qrCode, ed);
			this.save(ed);
		} else {
			this.insert(qrCode);
		}
	}

	private Lock lock = new ReentrantLock();

	@Override
	public String delete(String id) {
		try {
			lock.lock();
			// 删除图片
			QrCode de = this.findOneById(id, QrCode.class);
			MultiMedia qrcode = de.getQrcode();
			List<MultiMedia> delimg = new ArrayList<MultiMedia>();
			delimg.add(qrcode);
			de.setIsDelete(true);
			this.save(de);
			deleteImgs(delimg);
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return "error";
	}

	private void deleteImgs(List<MultiMedia> media) {
		if (media != null) {
			media.forEach(del -> {
				this.multiMediaService.deleteMultiMedia(del.getId(), del.getFileType());

			});
		}
	}

	@Override
	public QrCode findQrCodeById(String id) {
		Query query = new Query();
		if (Common.isNotEmpty(id)) {
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.addCriteria(Criteria.where("isDisable").is(false));
			query.addCriteria(Criteria.where("_id").is(new ObjectId(id)));
		}
		return this.findOneByQuery(query, QrCode.class);
	}

	

}
