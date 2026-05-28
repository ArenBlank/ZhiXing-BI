package com.huang.zhixing.parser.impl;

import com.huang.zhixing.parser.AbstractFileParser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component("xlsx")
public class XlsxFileParser extends AbstractFileParser {

    @Override
    public String getSupportedExtension() {
        return "xlsx";
    }

    @Override
    protected String doParse(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.forEach(sheet -> {
                sb.append("【工作表: ").append(sheet.getSheetName()).append("】\n");
                sheet.forEach(row -> {
                    row.forEach(cell -> {
                        switch (cell.getCellType()) {
                            case STRING  -> sb.append(cell.getStringCellValue());
                            case NUMERIC -> sb.append(cell.getNumericCellValue());
                            case BOOLEAN -> sb.append(cell.getBooleanCellValue());
                            default -> {}
                        }
                        sb.append("\t");
                    });
                    sb.append("\n");
                });
                sb.append("\n");
            });
        }
        return sb.toString();
    }
}
