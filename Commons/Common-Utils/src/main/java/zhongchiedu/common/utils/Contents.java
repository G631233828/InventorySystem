package zhongchiedu.common.utils;

import java.io.File;

/**
 * 系统常量配置
 * @author fliay
 *
 */
public interface Contents {
    

	public static final String USER_REDIS_SESSION = "user-redis-session";//api session
	public static final String USER_SESSION = "user_session";//后台用户session
    public static final String USER_SESSION_ID = "usersessionId";
    public static final String RESOURCES_LIST = "resourceslist";
    public static final String MENU_ID="menuid";
    public static final String SCHOOL_SESSION = "school_session";//学校session
    
    public static final String STOCK_LIST="stock_list";//批量出库session
    
    public static final String WECHAT_WEB_SESSION="wechat_session";//微信端session
    public static final String WECHAT_WEB_RESOURCES_LIST="wechat_web_resources";//微信端菜单
    
    /**
     * Realm名称
     */
    public static final String REALMNAME = "SHIRONAME";

	/**
	 * 地址栏id
	 */
    public static final String URLID="urlid";
    
    /**
     * 子目录资源
     */
    public static final String SUB_RESOURCE="subresource";
    /**
     * 父目录资源
     */
    public static final String SUP_RESOURCE="supresource";
    
    /**
     * 上次文件路径
     */
    public static final String NEWSFILE = File.separator+"assets"+File.separator+"FileUpload"+File.separator+"Img"+File.separator;
	
    
    /**
     * 上传文件
     */
    public static final String UPLOADPATH="uploadpath";
    public static final String FILENAME="filename";
    public static final String SAVEPATH="savepath";
    public static final String UPLOADDIR="uploaddir";//上传目录
    public static final String SUFFIXNAME="suffixname";//后缀名
    public static final String ERROR ="error";
    		
    
    
    /**
     * 微信配置文件读取
     */
    
    public static final String URL = ReadProperties.getObjectProperties("application.yml", "wechat.serverUrl");
    public static final String APPSECRET = ReadProperties.getObjectProperties("application.yml", "wechat.appsecret");
    public static final String APPID = ReadProperties.getObjectProperties("application.yml", "wechat.appid");
    
    
    
    
    
    

    /**********************************************分隔符常量************************************************/

    String POINT_STR = ".";

    String BLANK_STR = "";

    String SPACE_STR = " ";

    String NEWLINE_STR = "\n";

    String SYS_SEPARATOR = File.separator;

    String FILE_SEPARATOR = "/";

    String BRACKET_LEFT = "[";

    String BRACKET_RIGHT = "]";

    String UNDERLINE = "_";

    String MINUS_STR = "-";



    /**********************************************编码格式************************************************/

    String UTF8 = "UTF-8";


    /**********************************************文件后缀************************************************/

    String EXCEL_XLS = ".xls";

    String EXCEL_XLSX = ".xlsx";

    String IMAGE_PNG = "png";

    String IMAGE_JPG = "jpg";

    String FILE_ZIP = ".zip";
    String FILE_GZ = ".gz";


    /**********************************************io流************************************************/

    int BUFFER_1024 = 1024;

    int BUFFER_512 = 512;

    String USER_DIR = "user.dir";

    /**********************************************tesseract for java语言字库************************************************/

    String ENG = "eng";

    String CHI_SIM = "chi_sim";


    /**********************************************opencv************************************************/
    String OPENCV_LIB_NAME_246 = "libs/x64/opencv_java246";

    String OPENCV_LIB_NAME_330 = "libs/x64/opencv_java330";
    
    
    /**
     *     
     * @author fliay
     *	PUBLISHER 发布权限
     *	HANDLER 处理权限
     *  KNOWPEOPLE 知晓人
     */
        public enum inventoryRoles{
        	PUBLISHER,HANDLER,KNOWPEOPLE
        }
        
    

    
    
    
    
    
    
}
