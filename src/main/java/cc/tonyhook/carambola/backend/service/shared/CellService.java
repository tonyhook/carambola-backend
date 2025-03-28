package cc.tonyhook.carambola.backend.service.shared;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

@Service
public class CellService {

    public String getStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                return String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            } else {
                DecimalFormat df = new DecimalFormat("0.0000");
                String str = df.format(cell.getNumericCellValue());
                while (str.indexOf(".") >= 0 && str.endsWith("0")) {
                    str = str.substring(0, str.length() - 1);
                }
                if (str.endsWith(".")) {
                    str = str.substring(0, str.length() - 1);
                }
                return str;
            }
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().replaceAll("[\\s\\u00A0]+", " ").strip();
        }
        if (cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            CellValue cellValue = cell.getSheet().getWorkbook()
                .getCreationHelper()
                .createFormulaEvaluator()
                .evaluate(cell);

            if (cellValue.getCellType() != CellType.ERROR) {
                switch (cellValue.getCellType()) {
                    case NUMERIC:
                        DecimalFormat df = new DecimalFormat("0.0000");
                        String str = df.format(cellValue.getNumberValue());
                        while (str.indexOf(".") >= 0 && str.endsWith("0")) {
                            str = str.substring(0, str.length() - 1);
                        }
                        if (str.endsWith(".")) {
                            str = str.substring(0, str.length() - 1);
                        }
                        return str;
                    case STRING:
                        return cellValue.getStringValue().replaceAll("[\\s\\u00A0]+", " ").strip();
                    default:
                        return "";
                }
            } else {
                return "";
            }
        }

        return "";
    }

    public String getExcelColumnLetter(int column) {
        StringBuilder sb = new StringBuilder();
        while (column >= 0) {
            sb.insert(0, (char) ('A' + (column % 26)));
            column = column / 26 - 1;
        }
        return sb.toString();
    }

    public void adjustColumnWeight(Sheet sheet, int startColumnNum, int size) {
        for (int columnNum = 0; columnNum < size; columnNum++) {
            sheet.autoSizeColumn(columnNum);
            final int columnWidth = sheet.getColumnWidth(columnNum);
            if (columnNum >= 256 * 256) {
                continue;
            }
            int newWidth = columnWidth;
            for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row currentRow;
                if (sheet.getRow(rowNum) == null) {
                    continue;
                } else {
                    currentRow = sheet.getRow(rowNum);
                }
                if (currentRow.getCell(columnNum) != null) {
                    Cell currentCell = currentRow.getCell(columnNum);
                    String value = getStringValue(currentCell);
                    int count = chineseCharCounter(value);
                    int length = value.length() * 256 + count * 256 + 512;
                    if (newWidth < length && length < 256 * 256) {
                        newWidth = length;
                    }
                }
            }
            if (newWidth != columnWidth) {
                sheet.setColumnWidth(columnNum, newWidth);
            }
        }
    }

    private static int chineseCharCounter(String input) {
        int count = 0;
        if (input != null){
            String regEx = "[\\u4e00-\\u9fa5]";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(input);
            int len = m.groupCount();
            while (m.find()) {
                for (int i = 0; i <= len; i++) {
                    count = count + 1;
                }
            }
        }
        return count;
    }

}
