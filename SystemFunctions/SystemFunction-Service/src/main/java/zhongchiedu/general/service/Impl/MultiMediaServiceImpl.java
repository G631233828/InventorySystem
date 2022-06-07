package zhongchiedu.general.service.Impl;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.common.utils.ImageTool;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.MultiMediaService;
@Service
public class MultiMediaServiceImpl  extends GeneralServiceImpl<MultiMedia> implements MultiMediaService{

	@Autowired
	private FileOperateUtil fileOperateUtil;
	
	@Autowired
	private ImageTool imageTool;
	
	/**
	 * 上传文件
	 * @return
	 * dir  d:/
	 * path upload/images
	 */
	public List<MultiMedia> uploadPictures(MultipartFile[] file,String dir,String path,String belong,int width,int height){
		List<MultiMedia> list = new ArrayList<MultiMedia>();
		if (Common.isNotEmpty(file[0].getOriginalFilename())) {
			DecimalFormat d = new DecimalFormat("#.00");
			for (MultipartFile m : file) {
				String uploadPath = dir + path;
				MultiMedia multi = new MultiMedia();
				Map<String, Object> map = this.fileOperateUtil.upload(m, uploadPath, m.getOriginalFilename());
				String compName = map.get(Contents.FILENAME).toString();
				String com_Name = "comp_" + compName;
				imageTool.compressPic(uploadPath, uploadPath, compName, com_Name, width, height, true);
				multi.setCompressPicName(com_Name);
				multi.setOriginalName(m.getOriginalFilename());
				multi.setGenerateName(map.get(Contents.FILENAME).toString());
				multi.setSavePath(path);
				multi.setDir(dir);
				multi.setThumbnail(null);
				multi.setExtension(map.get(Contents.SUFFIXNAME).toString());
				multi.setFileType("IMG");
				double size = m.getSize();
				String sizeStr = size < 1023 ? "B"
						: size < (1024 * 1024) - 1 ? "KB" : size < (1024 * 1024 * 1024) - 1 ? "MB" : "GB";
				double fileSize = size < 1023 ? size
						: size < (1024 * 1024) - 1 ? (size / 1024)
								: size < (1024 * 1024 * 1024) - 1 ? (size / (1024 * 1024))
										: (size / (1024 * 1024 * 1024));
				multi.setFileSize(d.format(fileSize));
				multi.setFileSizeStr(sizeStr);
				multi.setBelong(belong);
				this.insert(multi);
				list.add(multi);
			}
		}
		return list;
		
	}
	

	/**
	 * 上传文件
	 * 
	 * @return
	 */
	public List<MultiMedia> uploadPictures(MultipartFile[] file, String dir, String path, String belong) {

		List<MultiMedia> list = new ArrayList<MultiMedia>();
		if (Common.isNotEmpty(file[0].getOriginalFilename())) {
			DecimalFormat d = new DecimalFormat("#.00");
			for (MultipartFile m : file) {
				System.out.println(m.getOriginalFilename());
				String uploadPath = dir + path;
				MultiMedia multi = new MultiMedia();
				Map<String, Object> map = this.fileOperateUtil.upload(m, uploadPath, m.getOriginalFilename());
				String compName = map.get(Contents.FILENAME).toString();
				imageTool.compressPic(uploadPath, uploadPath, compName, "comp_" + compName, 640, 320, true);
				multi.setCompressPicName("comp_" + compName);
				multi.setOriginalName(m.getOriginalFilename());
				multi.setGenerateName(map.get(Contents.FILENAME).toString());
				multi.setSavePath(path);
				multi.setDir(dir);
				multi.setThumbnail(null);
				multi.setExtension(map.get(Contents.SUFFIXNAME).toString());
				multi.setFileType("IMG");
				double size = m.getSize();
				String sizeStr = size < 1023 ? "B"
						: size < (1024 * 1024) - 1 ? "KB" : size < (1024 * 1024 * 1024) - 1 ? "MB" : "GB";
				double fileSize = size < 1023 ? size
						: size < (1024 * 1024) - 1 ? (size / 1024)
								: size < (1024 * 1024 * 1024) - 1 ? (size / (1024 * 1024))
										: (size / (1024 * 1024 * 1024));
				multi.setFileSize(d.format(fileSize));
				multi.setFileSizeStr(sizeStr);
				multi.setBelong(belong);
				this.insert(multi);
				list.add(multi);
			}
		}
		return list;

	}

	/**
	 * 上传文件
	 * 
	 * @return
	 */
	public MultiMedia uploadVideo(MultipartFile m, String dir, String path, String belong) {
		MultiMedia multi = null;
		if (Common.isNotEmpty(m.getOriginalFilename())) {
			DecimalFormat d = new DecimalFormat("#.00");
			multi = new MultiMedia();
			String uploadPath = dir + path;
			Map<String, Object> map = this.fileOperateUtil.upload(m, uploadPath, m.getOriginalFilename());
			multi.setOriginalName(m.getOriginalFilename());
			multi.setGenerateName(map.get(Contents.FILENAME).toString());
			multi.setSavePath(path);
			multi.setDir(dir);
			multi.setThumbnail(null);
			multi.setExtension(map.get(Contents.SUFFIXNAME).toString());
			multi.setFileType("Video");
			double size = m.getSize();
			String sizeStr = size < 1023 ? "B"
					: size < (1024 * 1024) - 1 ? "KB" : size < (1024 * 1024 * 1024) - 1 ? "MB" : "GB";
			double fileSize = size < 1023 ? size
					: size < (1024 * 1024) - 1 ? (size / 1024)
							: size < (1024 * 1024 * 1024) - 1 ? (size / (1024 * 1024)) : (size / (1024 * 1024 * 1024));
			multi.setFileSize(d.format(fileSize));
			multi.setFileSizeStr(sizeStr);
			multi.setBelong(belong);
			this.insert(multi);
		}
		return multi;

	}

	@Override
	public List<MultiMedia> findInIds(List<String> ids) {

		Query query = new Query();
		query.addCriteria(Criteria.where("_id").in(ids));
		return this.find(query, MultiMedia.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	public void deleteMultiMedia(String id, String type) {
		try {
			lock.lock();
			MultiMedia multiMedia = this.findOneById(id, MultiMedia.class);
			// 获取文件路径并且删除
			if (Common.isNotEmpty(multiMedia)) {
				String dir = multiMedia.getDir();
				String savePath = multiMedia.getSavePath();
				List<String> deleteFile = new ArrayList<String>();
				if (type.equals("VIDEO")) {
					String originalName = dir + savePath + "/real/" + multiMedia.getOriginalName();
					String encodingPath = dir + multiMedia.getEncodingPath() + multiMedia.getGenerateName();
					String thumbnaiName = dir + multiMedia.getThumbnail() + multiMedia.getThumbnailName();
					deleteFile.add(originalName);
					deleteFile.add(encodingPath);
					deleteFile.add(thumbnaiName);
				} else if (type.equals("IMG")) {
					// 删除缩略图，原图
					String generateName = dir + savePath + multiMedia.getGenerateName();
					String compressPicName = dir + savePath + multiMedia.getCompressPicName();
					deleteFile.add(generateName);
					deleteFile.add(compressPicName);
				}
				// 将文件移入删除目录
				Common.deleteFile(deleteFile, dir + savePath + "/");

			}
			multiMedia.setIsDelete(true);
			this.save(multiMedia);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}

	}

	@Override
	public MultiMedia saveQrCode(File file, String dir, String path,String type) {
		DecimalFormat d = new DecimalFormat("#.00");
		MultiMedia multi = new MultiMedia();
		multi.setOriginalName(file.getName());
		multi.setSavePath(path);
		multi.setDir(dir);
		multi.setBelong(type);
		multi.setExtension(Common.getSuffix(file.getName()));
		multi.setFileType("QRCODE");
		double size = file.length();
		String sizeStr = size < 1023 ? "B"
				: size < (1024 * 1024) - 1 ? "KB" : size < (1024 * 1024 * 1024) - 1 ? "MB" : "GB";
		double fileSize = size < 1023 ? size
				: size < (1024 * 1024) - 1 ? (size / 1024)
						: size < (1024 * 1024 * 1024) - 1 ? (size / (1024 * 1024)) : (size / (1024 * 1024 * 1024));
		multi.setFileSize(d.format(fileSize));
		multi.setFileSizeStr(sizeStr);
		this.insert(multi);
		return multi;
	}

	@Override
	public void editMultiMedia(String id, MultiMedia multiMedia) {

		if(Common.isNotEmpty(id)&&Common.isNotEmpty(multiMedia)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("_id").is(new ObjectId(id)));
			query.addCriteria(Criteria.where("isDisable").is(false));
			query.addCriteria(Criteria.where("isDelete").is(false));
			MultiMedia multi = this.findOneByQuery(query, MultiMedia.class);
			if(!Common.isEmpty(multi)) {
//				BeanUtils.copyProperties(multiMedia, multi);
				multi.setAuthor(multiMedia.getAuthor());
				multi.setTitle(multiMedia.getTitle());
				this.save(multi);
			}
		}
		
	}
}
