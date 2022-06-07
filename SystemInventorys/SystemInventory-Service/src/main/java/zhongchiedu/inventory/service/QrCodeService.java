package zhongchiedu.inventory.service;

import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.inventory.pojo.QrCode;

public interface QrCodeService extends GeneralService<QrCode> {

	
	Pagination<QrCode> findPagination(Integer pageNo, Integer pageSize);
	
	void saveOrUpdate(QrCode qrCode);
	
	String delete(String id);

	QrCode findQrCodeById(String id);
	
	
	
	
	
}
