package com.aec.cli;

import com.aec.application.KnowledgeService;
import com.aec.application.PullRequestReviewService;
import com.aec.application.TicketAnalysisService;
import com.aec.application.request.AnalyzeTicketCommand;
import com.aec.application.request.IngestKnowledgeCommand;
import com.aec.application.request.ReviewPullRequestCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.Quarkus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@TopCommand
@Command(
        name = "aec",
        mixinStandardHelpOptions = true,
        subcommands = {
                AecCli.AnalyzeTicketCommandCli.class,
                AecCli.ReviewPrCommandCli.class,
                AecCli.IngestKnowledgeCommandCli.class,
                AecCli.QueryKnowledgeCommandCli.class
        })
@ApplicationScoped
public class AecCli implements Runnable {

    @Override
    public void run() {
        System.out.println("HTTP service running. Use a subcommand for CLI mode or keep the app running for REST endpoints.");
        Quarkus.waitForExit();
    }

    @Command(name = "analyze-ticket", description = "Analyze a ticket from json, github, or jira.")
    @Unremovable
    @Dependent
    public static class AnalyzeTicketCommandCli implements Callable<Integer> {
        @Option(names = "--source", required = true)
        String source;

        @Option(names = "--id", description = "Ticket id, such as owner/repo#123 or ABC-123.")
        String id;

        @Option(names = "--file", description = "Local JSON file for --source=json.")
        String file;

        private final TicketAnalysisService service;
        private final ObjectMapper objectMapper;

        public AnalyzeTicketCommandCli(TicketAnalysisService service, ObjectMapper objectMapper) {
            this.service = service;
            this.objectMapper = objectMapper;
        }

        @Override
        public Integer call() throws Exception {
            String identifier = "json".equalsIgnoreCase(source) ? file : id;
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(service.analyze(new AnalyzeTicketCommand(source, identifier))));
            return 0;
        }
    }

    @Command(name = "review-pr", description = "Review a GitHub pull request.")
    @Unremovable
    @Dependent
    public static class ReviewPrCommandCli implements Callable<Integer> {
        @Option(names = "--owner", required = true)
        String owner;

        @Option(names = "--repo", required = true)
        String repo;

        @Option(names = "--number", required = true)
        int number;

        private final PullRequestReviewService service;
        private final ObjectMapper objectMapper;

        public ReviewPrCommandCli(PullRequestReviewService service, ObjectMapper objectMapper) {
            this.service = service;
            this.objectMapper = objectMapper;
        }

        @Override
        public Integer call() throws Exception {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(service.review(new ReviewPullRequestCommand(owner, repo, number))));
            return 0;
        }
    }

    @Command(name = "ingest-knowledge", description = "Ingest a knowledge document.")
    @Unremovable
    @Dependent
    public static class IngestKnowledgeCommandCli implements Callable<Integer> {
        @Option(names = "--title", required = true)
        String title;

        @Option(names = "--source", required = true)
        String source;

        @Option(names = "--content", required = true)
        String content;

        private final KnowledgeService service;
        private final ObjectMapper objectMapper;

        public IngestKnowledgeCommandCli(KnowledgeService service, ObjectMapper objectMapper) {
            this.service = service;
            this.objectMapper = objectMapper;
        }

        @Override
        public Integer call() throws Exception {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(service.ingest(new IngestKnowledgeCommand(title, source, content))));
            return 0;
        }
    }

    @Command(name = "query-knowledge", description = "Query the knowledge store.")
    @Unremovable
    @Dependent
    public static class QueryKnowledgeCommandCli implements Callable<Integer> {
        @Option(names = "--query", required = true)
        String query;

        private final KnowledgeService service;
        private final ObjectMapper objectMapper;

        public QueryKnowledgeCommandCli(KnowledgeService service, ObjectMapper objectMapper) {
            this.service = service;
            this.objectMapper = objectMapper;
        }

        @Override
        public Integer call() throws Exception {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(service.query(query)));
            return 0;
        }
    }
}
