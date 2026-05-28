package com.huang.zhixing.parser;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FileParserFactory {

    private final Map<String, AbstractFileParser> parserMap;

    public FileParserFactory(Map<String, AbstractFileParser> parserMap) {
        this.parserMap = parserMap;
    }

    public AbstractFileParser getParser(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("无法识别文件类型: " + fileName);
        }

        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        AbstractFileParser parser = parserMap.get(ext);
        if (parser == null) {
            throw new IllegalArgumentException("不支持的文件类型: ." + ext);
        }
        return parser;
    }
}
