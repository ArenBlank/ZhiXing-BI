package com.huang.zhixing.parser.impl;

import com.huang.zhixing.parser.AbstractFileParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component("pdf")
public class PdfFileParser extends AbstractFileParser {

    @Override
    public String getSupportedExtension() {
        return "pdf";
    }

    @Override
    protected String doParse(InputStream inputStream) throws Exception {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
}
