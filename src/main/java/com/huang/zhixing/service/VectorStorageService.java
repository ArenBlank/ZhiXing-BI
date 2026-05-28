package com.huang.zhixing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStorageService {

    private final VectorStore vectorStore;

    /**
     * 将文件内容切块、注入用户隔离元数据、存入 Redis-Stack 向量库
     */
    public void storeFileContent(String content, String userId, String sessionId, String fileName) {
        // 1. 语义切块（块更大 → 块更少 → 更快）
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(1500)
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(20)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.apply(List.of(new Document(content)));

        // 2. 强制注入隔离元数据 —— 去除连字符防止 RediSearch TAG 过滤语法错误
        String safeUserId = userId.replace("-", "");
        String safeSessionId = sessionId.replace("-", "");
        for (Document chunk : chunks) {
            chunk.getMetadata().put("userId", safeUserId);
            chunk.getMetadata().put("sessionId", safeSessionId);
            chunk.getMetadata().put("fileName", fileName);
        }

        // 3. 批量 Embedding + 存入 Redis-Stack（每块需调用 Ollama，约 2-3 秒/块）
        log.info("开始向量化: file={}, chunks={}, 预计耗时{}秒", fileName, chunks.size(), chunks.size() * 3);
        long start = System.currentTimeMillis();
        vectorStore.add(chunks);
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("知识沉淀完成: userId={}, sessionId={}, file={}, chunks={}, 耗时{}秒",
                userId, sessionId, fileName, chunks.size(), elapsed);
    }

    /**
     * 用户专属语义搜索 —— 通过 Filter.Expression 在 Redis-Stack 层硬隔离
     */
    public List<Document> searchUserKnowledge(String query, String userId, String sessionId, int topK) {
        String safeUserId = userId.replace("-", "");
        String safeSessionId = sessionId.replace("-", "");
        Filter.Expression filterExpr = new Filter.Expression(
                Filter.ExpressionType.AND,
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("userId"),
                        new Filter.Value(safeUserId)
                ),
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("sessionId"),
                        new Filter.Value(safeSessionId)
                )
        );

        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withFilterExpression(filterExpr);

        return vectorStore.similaritySearch(request);
    }
}
