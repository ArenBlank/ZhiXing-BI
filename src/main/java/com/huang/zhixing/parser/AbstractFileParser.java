package com.huang.zhixing.parser;

import com.huang.zhixing.model.dto.FileParseResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public abstract class AbstractFileParser {

    public abstract String getSupportedExtension();

    public final FileParseResultDTO parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String content;
        try (InputStream is = file.getInputStream()) {
            content = doParse(is);
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败: " + e.getMessage(), e);
        }

        FileParseResultDTO result = new FileParseResultDTO();
        result.setContent(content);
        result.setFileName(file.getOriginalFilename());
        result.setFileType(getSupportedExtension());
        result.setFileSize(file.getSize());
        return result;
    }

    protected abstract String doParse(InputStream inputStream) throws Exception;
}
