package com.huang.zhixing.parser.impl;

import com.huang.zhixing.parser.AbstractFileParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component("md")
public class MarkdownFileParser extends AbstractFileParser {

    @Override
    public String getSupportedExtension() {
        return "md";
    }

    @Override
    protected String doParse(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
