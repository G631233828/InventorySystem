package zhongchiedu.common.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Repository;

/**
 * Excle 表格的导入
 * @author mac
 */
@Repository
public class ExcelReadUtil {

    // 对外提供读取excel文件的接口
    public static String[][] readExcel(File file, int ignoreRows) throws IOException {
        String fName = file.getName();
        String extension = fName.lastIndexOf(".") == -1 ? "" : fName
                .substring(fName.lastIndexOf(".") + 1);
        if ("xls".equals(extension)) {// 2003
            System.err.println("读取excel2003文件内容");
            return read2003Excel(file, ignoreRows);
        } else if ("xlsx".equals(extension)) {// 2007
            System.err.println("读取excel2007文件内容");
            return read2007Excel(file, ignoreRows);
        } else {
            throw new IOException("不支持的文件类型:" + extension);
        }
    }

    /**
     * 读取2003版Excel（.xls）
     * 核心修复：公式单元格按计算结果类型取值，避免字符串/数值类型冲突
     */
    public static String[][] read2003Excel(File file, int ignoreRows)
            throws FileNotFoundException, IOException {
        List<String[]> result = new ArrayList<String[]>();
        int rowSize = 0;
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        POIFSFileSystem fs = new POIFSFileSystem(in);
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFCell cell = null;

        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
            HSSFSheet st = wb.getSheetAt(sheetIndex);
            // 跳过忽略行，读取数据行
            for (int rowIndex = ignoreRows; rowIndex <= st.getLastRowNum(); rowIndex++) {
                HSSFRow row = st.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int tempRowSize = row.getLastCellNum() + 1;
                if (tempRowSize > rowSize) {
                    rowSize = tempRowSize;
                }
                String[] values = new String[rowSize];
                Arrays.fill(values, "");
                boolean hasValue = false;

                for (short columnIndex = 0; columnIndex <= row.getLastCellNum(); columnIndex++) {
                    String value = "";
                    cell = row.getCell(columnIndex);
                    if (cell != null) {
                        // 按单元格类型正确取值（核心修复：公式单元格处理）
                        switch (cell.getCellType()) {
                            case HSSFCell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                break;
                            case HSSFCell.CELL_TYPE_NUMERIC:
                                // 日期类型处理
                                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                    Date date = cell.getDateCellValue();
                                    value = date != null ? new SimpleDateFormat("yyyy-MM-dd").format(date) : "";
                                } else {
                                    // 数值转字符串（避免科学计数法）
                                    value = new DecimalFormat("0.##").format(cell.getNumericCellValue());
                                }
                                break;
                            case HSSFCell.CELL_TYPE_FORMULA:
                                // 修复核心：先获取公式计算结果类型，再取值
                                value = getFormulaCellValue(cell);
                                break;
                            case HSSFCell.CELL_TYPE_BLANK:
                                value = "";
                                break;
                            case HSSFCell.CELL_TYPE_ERROR:
                                value = "";
                                break;
                            case HSSFCell.CELL_TYPE_BOOLEAN:
                                value = cell.getBooleanCellValue() ? "Y" : "N";
                                break;
                            default:
                                value = "";
                        }
                    }
                    // 第一列空则跳过整行
                    if (columnIndex == 0 && value.trim().equals("")) {
                        break;
                    }
                    values[columnIndex] = rightTrim(value);
                    hasValue = true;
                }
                if (hasValue) {
                    result.add(values);
                }
            }
        }
        in.close();
        // 转换为二维数组
        String[][] returnArray = new String[result.size()][rowSize];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = result.get(i);
        }
        return returnArray;
    }

    /**
     * 读取2007版Excel（.xlsx）
     * 核心修复：公式单元格按计算结果类型取值，统一数值/字符串处理逻辑
     */
    public static String[][] read2007Excel(File file, int ignoreRows)
            throws FileNotFoundException, IOException {
        List<String[]> result = new ArrayList<String[]>();
        int rowSize = 0;
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        XSSFWorkbook wb = new XSSFWorkbook(in);
        XSSFCell cell = null;

        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
            XSSFSheet st = wb.getSheetAt(sheetIndex);
            // 跳过忽略行，读取数据行
            for (int rowIndex = ignoreRows; rowIndex <= st.getLastRowNum(); rowIndex++) {
                XSSFRow row = st.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int tempRowSize = row.getLastCellNum() + 1;
                if (tempRowSize > rowSize) {
                    rowSize = tempRowSize;
                }
                String[] values = new String[rowSize];
                Arrays.fill(values, "");
                boolean hasValue = false;

                for (short columnIndex = 0; columnIndex <= row.getLastCellNum(); columnIndex++) {
                    String value = "";
                    cell = row.getCell(columnIndex);
                    if (cell != null) {
                        // 按单元格类型正确取值（核心修复：公式单元格处理）
                        switch (cell.getCellType()) {
                            case XSSFCell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                break;
                            case XSSFCell.CELL_TYPE_NUMERIC:
                                // 日期类型处理
                                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                    Date date = cell.getDateCellValue();
                                    value = date != null ? new SimpleDateFormat("yyyy-MM-dd").format(date) : "";
                                } else {
                                    // 数值转字符串（避免科学计数法）
                                    value = new DecimalFormat("0.##").format(cell.getNumericCellValue());
                                }
                                break;
                            case XSSFCell.CELL_TYPE_FORMULA:
                                // 修复核心：先获取公式计算结果类型，再取值
                                value = getXSSFFormulaCellValue(cell);
                                break;
                            case XSSFCell.CELL_TYPE_BLANK:
                                value = "";
                                break;
                            case XSSFCell.CELL_TYPE_ERROR:
                                value = "";
                                break;
                            case XSSFCell.CELL_TYPE_BOOLEAN:
                                value = cell.getBooleanCellValue() ? "Y" : "N";
                                break;
                            default:
                                value = "";
                        }
                    }
                    // 第一列空则置空（保留原有逻辑）
                    if (columnIndex == 0 && value.trim().equals("")) {
                        value = "";
                    }
                    values[columnIndex] = rightTrim(value);
                    hasValue = true;
                }
                if (hasValue) {
                    result.add(values);
                }
            }
        }
        in.close();
        // 转换为二维数组
        String[][] returnArray = new String[result.size()][rowSize];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = result.get(i);
        }
        return returnArray;
    }

    /**
     * 处理HSSF（2003）公式单元格值：根据计算结果类型取值
     */
    private static String getFormulaCellValue(HSSFCell cell) {
        String value = "";
        try {
            // 获取公式计算结果的类型
            CellType resultType = cell.getCachedFormulaResultTypeEnum();
            switch (resultType) {
                case NUMERIC:
                    // 数值型公式结果
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        value = date != null ? new SimpleDateFormat("yyyy-MM-dd").format(date) : "";
                    } else {
                        value = new DecimalFormat("0.##").format(cell.getNumericCellValue());
                    }
                    break;
                case STRING:
                    // 字符串型公式结果
                    value = cell.getStringCellValue();
                    break;
                case BOOLEAN:
                    // 布尔型公式结果
                    value = cell.getBooleanCellValue() ? "Y" : "N";
                    break;
                case ERROR:
                    // 公式计算错误
                    value = "";
                    break;
                default:
                    value = "";
            }
        } catch (Exception e) {
            // 兜底：若获取缓存结果失败，直接计算公式（POI高版本支持）
            try {
                value = new DecimalFormat("0.##").format(cell.getNumericCellValue());
            } catch (Exception ex) {
                value = "";
            }
        }
        return value;
    }

    /**
     * 处理XSSF（2007）公式单元格值：根据计算结果类型取值
     */
    private static String getXSSFFormulaCellValue(XSSFCell cell) {
        String value = "";
        try {
            // 获取公式计算结果的类型
            CellType resultType = cell.getCachedFormulaResultTypeEnum();
            switch (resultType) {
                case NUMERIC:
                    // 数值型公式结果
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        value = date != null ? new SimpleDateFormat("yyyy-MM-dd").format(date) : "";
                    } else {
                        value = new DecimalFormat("0.##").format(cell.getNumericCellValue());
                    }
                    break;
                case STRING:
                    // 字符串型公式结果
                    value = cell.getStringCellValue();
                    break;
                case BOOLEAN:
                    // 布尔型公式结果
                    value = cell.getBooleanCellValue() ? "Y" : "N";
                    break;
                case ERROR:
                    // 公式计算错误
                    value = "";
                    break;
                default:
                    value = "";
            }
        } catch (Exception e) {
            // 兜底：若获取缓存结果失败，直接计算公式
            try {
                value = new DecimalFormat("0.##").format(cell.getNumericCellValue());
            } catch (Exception ex) {
                value = "";
            }
        }
        return value;
    }

    /**
     * 去掉字符串右边的空格
     */
    public static String rightTrim(String str) {
        if (str == null) {
            return "";
        }
        int length = str.length();
        for (int i = length - 1; i >= 0; i--) {
            if (str.charAt(i) != 0x20) {
                break;
            }
            length--;
        }
        return str.substring(0, length);
    }

    /**
     * 导出excel（保留原有逻辑，未修改）
     */
    @SuppressWarnings("rawtypes")
    public HSSFWorkbook export(List excelHeader, List<?> list, String sheetName) {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet(sheetName);
        HSSFRow row = sheet.createRow((int) 0);
        HSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        for (int i = 0; i < excelHeader.size(); i++) {
            HSSFCell cell = row.createCell(i);
            cell.setCellValue(excelHeader.get(i).toString());
            cell.setCellStyle(style);
            sheet.autoSizeColumn(i);
        }

        for (int i = 0; i < list.size(); i++) {
            row = sheet.createRow(i + 1);
            for (int j = 0; j < excelHeader.size(); j++) {
                row.createCell(j).setCellValue(1);
            }
        }
        return wb;
    }
}