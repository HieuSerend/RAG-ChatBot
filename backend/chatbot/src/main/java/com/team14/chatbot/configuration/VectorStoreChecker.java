package com.team14.chatbot.configuration;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VectorStoreChecker implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore knowledgeBaseVectorStore;
    private final VectorStore chatMemoryVectorStore;

    public VectorStoreChecker(
            JdbcTemplate jdbcTemplate,
            @Qualifier("knowledgeBaseVectorStore") VectorStore knowledgeBaseVectorStore,
            @Qualifier("chatMemoryVectorStore") VectorStore chatMemoryVectorStore
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.knowledgeBaseVectorStore = knowledgeBaseVectorStore;
        this.chatMemoryVectorStore = chatMemoryVectorStore;
    }

    @Override
    public void run(String... args) {
        System.out.println("🧠 Checking VectorStore connections...");
        checkTable("kb_embeddings_view", knowledgeBaseVectorStore);
        checkTable("chat_memory_embeddings", chatMemoryVectorStore);
        System.out.println("✅ VectorStore health check completed.");
    }

    private void checkTable(String tableName, VectorStore vectorStore) {
        try {
            // 1️⃣ Kiểm tra kết nối DB
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            // 2️⃣ Kiểm tra bảng tồn tại
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);

            // 3️⃣ Thử thực hiện một thao tác với VectorStore
            var testResults = vectorStore.similaritySearch("health check");

            System.out.printf("✅ [%s] OK — DB connected, table found, similaritySearch returns %d result(s)%n",
                    tableName, testResults.size());

        } catch (Exception e) {
            System.err.printf("❌ [%s] ERROR — %s%n", tableName, e.getMessage());
        }
    }
}
