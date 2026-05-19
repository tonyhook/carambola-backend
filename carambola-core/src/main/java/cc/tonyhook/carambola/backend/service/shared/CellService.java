package cc.tonyhook.carambola.backend.service.shared;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

    public String getStringValue(Element cell) {
        if (cell == null) {
            return null;
        }

        return cell.text().replaceAll("[\\s\\u00A0]+", " ").strip();
    }

    public Long getLongValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        return getLongValue(getStringValue(cell));
    }

    public Long getLongValue(Element cell) {
        if (cell == null) {
            return null;
        }

        return getLongValue(getStringValue(cell));
    }

    private Long getLongValue(String str) {
                if (str == null || str.isBlank()) {
            return null;
        }

        try {
            return Math.round(Double.parseDouble(str.replace(",", "")));
        } catch (Exception e) {
            return null;
        }
    }

    public Calendar getDateValue(Cell cell, String timezone) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.STRING) {
            return getDateValue(getStringValue(cell), timezone);
        } else if (cell.getCellType() == CellType.NUMERIC) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                TimeZone tz = TimeZone.getTimeZone(timezone);
                df.setTimeZone(tz);

                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    Calendar calendar = Calendar.getInstance(tz);
                    calendar.clear();
                    calendar.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
                    return calendar;
                } else {
                    long n = Math.round(cell.getNumericCellValue());
                    String date = n / 10000 + "-" + n % 10000 / 100 + "-" + n % 100;
                    long time = df.parse(date).getTime();
                    Calendar calendar = Calendar.getInstance(tz);
                    calendar.setTimeInMillis(time);
                    return calendar;
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    public Calendar getDateValue(Element cell, String timezone) {
        if (cell == null) {
            return null;
        }

        return getDateValue(getStringValue(cell), timezone);
    }

    public Calendar getDateValue(String str, String timezone) {
        if (str == null) {
            return null;
        }

        SimpleDateFormat df1 = new SimpleDateFormat("yyyy/M/d");
        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df3 = new SimpleDateFormat("yyyyMMdd");
        TimeZone tz = TimeZone.getTimeZone(timezone);
        df1.setTimeZone(tz);
        df2.setTimeZone(tz);
        df3.setTimeZone(tz);

        try {
            long time = df1.parse(str).getTime();
            Calendar calendar = Calendar.getInstance(tz);
            calendar.setTimeInMillis(time);
            return calendar;
        } catch (Exception e1) {
            try {
                long time = df2.parse(str).getTime();
                Calendar calendar = Calendar.getInstance(tz);
                calendar.setTimeInMillis(time);
                return calendar;
            } catch (Exception e2) {
                try {
                    long time = df3.parse(str).getTime();
                    Calendar calendar = Calendar.getInstance(tz);
                    calendar.setTimeInMillis(time);
                    return calendar;
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    public Double getDoubleValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        return getDoubleValue(getStringValue(cell));
    }

    public Double getDoubleValue(Element cell) {
        if (cell == null) {
            return null;
        }

        return getDoubleValue(getStringValue(cell));
    }

    private Double getDoubleValue(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(str.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public Element getHtmlCell(Elements cells, Integer index) {
        if (index == null || index < 0 || index >= cells.size()) {
            return null;
        }

        return cells.get(index);
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
