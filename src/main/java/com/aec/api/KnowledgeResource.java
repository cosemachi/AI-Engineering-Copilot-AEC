package com.aec.api;

import com.aec.application.KnowledgeService;
import com.aec.application.request.IngestKnowledgeCommand;
import com.aec.domain.IngestionJob;
import com.aec.domain.KnowledgeDocumentRecord;
import com.aec.domain.KnowledgeIngestResult;
import com.aec.domain.KnowledgeQueryResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/knowledge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KnowledgeResource {

    private final KnowledgeService knowledgeService;

    public KnowledgeResource(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @POST
    @Path("/ingest")
    public Response ingest(@Valid KnowledgeIngestRequest request) {
        KnowledgeIngestResult result =
                knowledgeService.ingest(new IngestKnowledgeCommand(request.title(), request.source(), request.content()));
        return Response.accepted(result).build();
    }

    @POST
    @Path("/query")
    public KnowledgeQueryResult query(@Valid KnowledgeQueryRequest request) {
        return knowledgeService.query(request.query());
    }

    @GET
    @Path("/documents/{id}")
    public KnowledgeDocumentRecord document(@PathParam("id") UUID id) {
        return knowledgeService.document(id);
    }

    @GET
    @Path("/jobs/{id}")
    public IngestionJob job(@PathParam("id") UUID id) {
        return knowledgeService.job(id);
    }

    public record KnowledgeIngestRequest(
            @NotBlank(message = "title is required") String title,
            @NotBlank(message = "source is required") String source,
            @NotBlank(message = "content is required") String content) {
    }

    public record KnowledgeQueryRequest(@NotBlank(message = "query is required") String query) {
    }
}
