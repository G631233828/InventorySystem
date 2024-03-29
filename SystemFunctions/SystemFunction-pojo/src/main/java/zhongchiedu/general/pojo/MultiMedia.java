package zhongchiedu.general.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultiMedia extends GeneralBean<MultiMedia>{/**
	 * 
	 */
	private static final long serialVersionUID = -6429735139697997563L;

	private String originalName;//源文件名称
	private String generateName;//生成文件名称
	private String savePath;//文件保存物理地址
	private String thumbnail;//缩略图
	private String encodingPath;//解码后的路径
	private String compressPicName;//文件的压缩地址
	private String dir;//文件卷
	private String extension;//文件扩展名
	private String fileType;//文件类别
	private String fileSize;//文件大小
	private String fileSizeStr;//单位 MB KB B
	private String belong;//归属
	private String thumbnailName;//缩略图名称
	private String videoTime;//视频时间
	private int views;//浏览次数
	private String title;
	private String author;
	
	
	
	
	
}
