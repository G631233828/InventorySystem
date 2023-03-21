package zhongchiedu.common.utils;


import org.springframework.stereotype.Repository;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

@Repository
public class PinyinTool {
	HanyuPinyinOutputFormat format = null;  
    public static enum Type {  
        UPPERCASE,              //全部大写  
        LOWERCASE,              //全部小写  
        FIRSTUPPER              //首字母大写  
    }  
    
    public PinyinTool(){  
        format = new HanyuPinyinOutputFormat();  
        format.setCaseType(HanyuPinyinCaseType.UPPERCASE);  
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);  
    }  
  
    public String toPinYin(String str) throws BadHanyuPinyinOutputFormatCombination{  
        return toPinYin(str, "", Type.FIRSTUPPER);  
    }  
  
    public String toPinYin(String str,String spera) throws BadHanyuPinyinOutputFormatCombination{  
        return toPinYin(str, spera, Type.FIRSTUPPER);  
    }  
  
    /** 
     * 将str转换成拼音，如果不是汉字或者没有对应的拼音，则不作转换 
     * 如： 明天 转换成 MINGTIAN 
     * @param str 
     * @param spera 
     * @return 
     * @throws BadHanyuPinyinOutputFormatCombination 
     */  
    public String toPinYin(String str, String spera, Type type) throws BadHanyuPinyinOutputFormatCombination {  
        if(str == null || str.trim().length()==0)  
            return "";  
        if(type == Type.UPPERCASE)  
            format.setCaseType(HanyuPinyinCaseType.UPPERCASE);  
        else  
            format.setCaseType(HanyuPinyinCaseType.LOWERCASE);  
  
        String py = "";  
        String temp = "";  
        String[] t;  
        for(int i=0;i<str.length();i++){  
            char c = str.charAt(i);  
            if((int)c <= 128)  
                py += c;  
            else{  
                t = PinyinHelper.toHanyuPinyinStringArray(c, format);  
                if(t == null)  
                    py += c;  
                else{  
                    temp = t[0];  
                    if(type == Type.FIRSTUPPER)  
                        temp = t[0].toUpperCase().charAt(0)+temp.substring(1);  
                    py += temp+(i==str.length()-1?"":spera);  
                }  
            }  
        }  
        return py.trim();  
    }



    public static String getPinYinHeadChar(String str) {
        if (isNull(str)) {
            return "";
        }
        String convert = "";
        for (int j = 0; j < str.length(); j++) {
            char word = str.charAt(j);
            // 提取汉字的首字母
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
            if (pinyinArray != null) {
                convert += pinyinArray[0].charAt(0);
            }
            else {
                convert += word;
            }
        }

        convert = string2AllTrim(convert);
        return convert.toUpperCase();
    }

    /*
     * 判断字符串是否为空
     */

    public static boolean isNull(Object strData) {
        if (strData == null || String.valueOf(strData).trim().equals("")) {
            return true;
        }
        return false;
    }

    /**
     * 去掉字符串包含的所有空格
     *
     * @param value
     * @return
     */
    public static String string2AllTrim(String value) {
        if (isNull(value)) {
            return "";
        }
        return value.trim().replace(" ", "");
    }
    
    public static void main(String[] args) throws Exception {
    	PinyinTool p = new PinyinTool();
    	String a = p.toPinYin("斯fsd蒂芬", "", Type.LOWERCASE);
    	
    	System.out.println(a);
	}

    
    
    
    
    
    
    
    
}
