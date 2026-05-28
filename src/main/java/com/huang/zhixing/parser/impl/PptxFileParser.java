package com.huang.zhixing.parser.impl;

import com.huang.zhixing.parser.AbstractFileParser;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component("pptx")
public class PptxFileParser extends AbstractFileParser {

    @Override
    public String getSupportedExtension() {
        return "pptx";
    }

    @Override
    protected String doParse(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
            int slideNum = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                sb.append("【幻灯片 ").append(slideNum++).append("】\n");
                for (var shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        sb.append(textShape.getText()).append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
