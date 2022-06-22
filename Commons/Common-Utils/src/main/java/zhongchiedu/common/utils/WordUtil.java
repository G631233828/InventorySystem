package zhongchiedu.common.utils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import cn.afterturn.easypoi.word.WordExportUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class WordUtil {

	private static Configuration configuration = null;

	private WordUtil() {
		throw new AssertionError();
	}

	/**
	 * 根据模板生成相应的文件
	 * 
	 * @param root     保存数据的map
	 * @param template 模板文件的地址
	 * @param path     生成的word文档输出地址
	 * @return
	 */
	public static synchronized ByteArrayOutputStream process(Map<?, ?> root, String templateName,String templatePath) {

		if (null == root) {
			throw new RuntimeException("数据不能为空");
		}

		if (null == templateName) {
			throw new RuntimeException("模板文件不能为空");
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		String templatePath = template.substring(0, template.lastIndexOf("/"));
//		String templateName = template.substring(template.lastIndexOf("/") + 1, template.length());

		if (null == configuration) {
			configuration = new Configuration(Configuration.VERSION_2_3_23); // 这里Configurantion对象不能有两个，否则多线程访问会报错
			configuration.setDefaultEncoding("utf-8");
			configuration.setClassicCompatible(true);
		}
//		configuration.setClassForTemplateLoading(WordUtil.class, templatePath);
//

		/*
		 * 以下是两种指定ftl文件所在目录路径的方式, 注意这两种方式都是 指定ftl文件所在目录的路径,而不是ftl文件的路径
		 */
		// 指定路径的第一种方式(根据某个类的相对路径指定)
		// configuration.setClassForTemplateLoading(this.getClass(),"");

		// 指定路径的第二种方式,我的路径是C:/a.ftl
		try {
			configuration.setDirectoryForTemplateLoading(new File(templatePath));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		
		
		Template t = null;
		try {
			t = configuration.getTemplate(templateName,"utf-8");
			Writer w = new BufferedWriter(new OutputStreamWriter(outputStream, "utf-8"));
			t.process(root, w); // 这里w是一个输出地址，可以输出到任何位置，如控制台，网页等
			w.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return outputStream;
	}
	
	

    /** 
     * 导出word（2007版本docx） 
     * 
     * @param templateWordPath 
     * @param dataMap 
     * @return 
     * @throws Exception 
     */ 
    public static byte[] exportWord(String templateWordPath, Map<String, Object> dataMap) throws Exception { 
        File tf = new File(templateWordPath); 
        if (!tf.exists() || !tf.isFile()) { 
            throw new RuntimeException("File [" + templateWordPath + "] Not Found Or Not File."); 
        } 
        XWPFDocument document = WordExportUtil.exportWord07(templateWordPath, dataMap); 
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        document.write(bos); 
        return bos.toByteArray(); 
    } 
 
	
}

