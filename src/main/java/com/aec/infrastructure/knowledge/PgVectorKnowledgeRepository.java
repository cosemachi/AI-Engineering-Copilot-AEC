package com.aec.infrastructure.knowledge;

import com.aec.application.AecException;
import com.aec.application.port.KnowledgeRepository;
import com.aec.domain.KnowledgeDocument;
import com.aec.domain.KnowledgeSnippet;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@IfBuildProperty(name = "aec.knowledge.repository", stringValue = "pgvector")
public class PgVectorKnowledgeRepository implements KnowledgeRepository {

    private final DataSource dataSource;

    public PgVectorKnowledgeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String repositoryName() {
        return "pgvector";
    }

    @Override
    public void save(KnowledgeDocument document) {
        String sql = "insert into knowledge_document (id, title, source, content, embedding) values (?, ?, ?, ?, ?::vector)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, document.id());
            statement.setString(2, document.title());
            statement.setString(3, document.source());
            statement.setString(4, document.content());
            statement.setString(5, vectorLiteral(document.embedding()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new AecException("Failed to save knowledge document to pgvector", e);
        }
    }

    @Override
    public List<KnowledgeSnippet> search(List<Double> embedding, int limit) {
        String sql = """
                select title, source, left(content, 180) as excerpt, 1 - (embedding <=> ?::vector) as score
                from knowledge_document
                order by embedding <=> ?::vector
                limit ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String vector = vectorLiteral(embedding);
            statement.setString(1, vector);
            statement.setString(2, vector);
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<KnowledgeSnippet> snippets = new ArrayList<>();
                while (resultSet.next()) {
                    snippets.add(new KnowledgeSnippet(
                            resultSet.getString("title"),
                            resultSet.getString("source"),
                            resultSet.getString("excerpt"),
                            resultSet.getDouble("score")));
                }
                return snippets;
            }
        } catch (SQLException e) {
            throw new AecException("Failed to query pgvector knowledge store", e);
        }
    }

    private String vectorLiteral(List<Double> embedding) {
        return "[" + embedding.stream()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "]";
    }
}
