package com.huang.zhixing.controller;

import com.huang.zhixing.common.Result;
import com.huang.zhixing.common.UserContext;
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

    @PostMapping("/upload")
    public Result<FileParseResultDTO> upload(@RequestParam("file") MultipartFile file) {
        AbstractFileParser parser = parserFactory.getParser(file.getOriginalFilename());
        return Result.success(parser.parse(file));
    }

    @PostMapping("/upload-and-index")
    public Result<FileParseResultDTO> uploadAndIndex(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {
        String userId = UserContext.get();
        AbstractFileParser parser = parserFactory.getParser(file.getOriginalFilename());
        FileParseResultDTO result = parser.parse(file);
        vectorStorageService.storeFileContent(result.getContent(), userId, sessionId, file.getOriginalFilename());
        return Result.success(result);
    }

    @PostMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam("query") String query,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "topK", defaultValue = "4") int topK) {
        String userId = UserContext.get();
        List<Document> docs = vectorStorageService.searchUserKnowledge(query, userId, sessionId, topK);
        return Result.success(docs.stream().map(doc -> Map.of("content", doc.getContent(), "metadata", doc.getMetadata())).toList());
    }
}
