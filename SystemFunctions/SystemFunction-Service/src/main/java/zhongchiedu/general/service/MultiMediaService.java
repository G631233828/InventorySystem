package zhongchiedu.general.service;

import java.io.File;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.framework.service.GeneralService;
import zhongchiedu.general.pojo.MultiMedia;

public interface MultiMediaService extends GeneralService<MultiMedia> {

	
	public List<MultiMedia> uploadPictures(MultipartFile[] file,String dir,String path,String belong,int width,int height);
	
 List<MultiMedia> uploadPictures(MultipartFile[] file,String dir,String path,String belong);
	 
	 public List<MultiMedia> findInIds(List<String> ids);
	 
	 public void deleteMultiMedia(String id,String type);
	 
	 public MultiMedia saveQrCode(File file,String dir,String path,String type);
	 
	 public void editMultiMedia(String id,MultiMedia multiMedia);
	
	
	
}
