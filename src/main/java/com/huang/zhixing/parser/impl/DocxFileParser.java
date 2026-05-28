package com.huang.zhixing.parser.impl;

import com.huang.zhixing.parser.AbstractFileParser;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component("docx")
public class DocxFileParser extends AbstractFileParser {

    @Override
    public String getSupportedExtension() {
        return "docx";
    }

    @Override
    protected String doParse(InputStream inputStream) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
