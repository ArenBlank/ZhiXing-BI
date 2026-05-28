package com.huang.zhixing.controller;

import com.huang.zhixing.common.Result;
import com.huang.zhixing.model.dto.FileParseResultDTO;
import com.huang.zhixing.parser.AbstractFileParser;
import com.huang.zhixing.parser.FileParserFactory;
import com.huang.zhixing.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileParserController {

    private final FileParserFactory parserFactory;
    private final VectorStorageService vectorStorageService;

    /**
     * 纯文件解析（不入库）
     */
    @PostMapping("/upload")
    public Result<FileParseResultDTO> upload(@RequestParam("file") MultipartFile file) {
        AbstractFileParser parser = parserFactory.getParser(file.getOriginalFilename());
        FileParseResultDTO result = parser.parse(file);
        return Result.success(result);
    }

    /**
     * 文件解析 + 自动向量入库（全自动知识沉淀）
     */
    @PostMapping("/upload-and-index")
    public Result<FileParseResultDTO> uploadAndIndex(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("sessionId") String sessionId) {
        AbstractFileParser parser = parserFactory.getParser(file.getOriginalFilename());
        FileParseResultDTO result = parser.parse(file);

        vectorStorageService.storeFileContent(
                result.getContent(), userId, sessionId, file.getOriginalFilename());

        return Result.success(result);
    }

    /**
     * 用户专属语义搜索（带 userId/sessionId 硬隔离）
     */
    @PostMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam("query") String query,
            @RequestParam("userId") String userId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "topK", defaultValue = "4") int topK) {
        List<Document> docs = vectorStorageService.searchUserKnowledge(query, userId, sessionId, topK);

        List<Map<String, Object>> results = docs.stream()
                .map(doc -> Map.<String, Object>of(
                        "content", doc.getContent(),
                        "metadata", doc.getMetadata()
                ))
                .toList();

        return Result.success(results);
    }
}
