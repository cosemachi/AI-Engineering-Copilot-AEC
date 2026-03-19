package com.aec.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

@QuarkusTest
class KnowledgeResourceTest {

    @Test
    void shouldQueueIngestAndQueryKnowledge() throws Exception {
        Response queued = given()
                .contentType("application/json")
                .body("""
                        {
                          "title": "Cache Strategy",
                          "source": "docs",
                          "content": "Use cache entries as derived data, never as the system of record."
                        }
                        """)
                .when()
                .post("/knowledge/ingest")
                .then()
                .statusCode(202)
                .body("status", containsString("queued"))
                .extract()
                .response();
        String documentId = queued.path("documentId");
        String jobId = queued.path("jobId");

        waitForJob(jobId);

        given()
                .contentType("application/json")
                .body("""
                        {
                          "query": "cache strategy"
                        }
                        """)
                .when()
                .post("/knowledge/query")
                .then()
                .statusCode(200)
                .body("answer", containsString("cache strategy"))
                .body("relevantDocuments", hasSize(1))
                .body("relevantDocuments[0].title", containsString("Cache Strategy"));

        given()
                .when()
                .get("/knowledge/documents/" + documentId)
                .then()
                .statusCode(200)
                .body("ingestionStatus", containsString("indexed"));
    }

    @Test
    void shouldRejectInvalidIngestPayload() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "title": null,
                          "source": "docs",
                          "content": null
                        }
                        """)
                .when()
                .post("/knowledge/ingest")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturnNotFoundForMissingDocumentAndJob() {
        given()
                .when()
                .get("/knowledge/documents/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .body("error", containsString("Knowledge document not found"));

        given()
                .when()
                .get("/knowledge/jobs/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .body("error", containsString("Ingestion job not found"));
    }

    private void waitForJob(String jobId) throws Exception {
        for (int i = 0; i < 20; i++) {
            ValidatableResponse response = given()
                    .when()
                    .get("/knowledge/jobs/" + jobId)
                    .then()
                    .statusCode(200);
            String status = response.extract().path("status");
            if ("succeeded".equals(status)) {
                return;
            }
            if ("failed".equals(status)) {
                throw new IllegalStateException("Knowledge ingestion job failed");
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Knowledge ingestion job did not finish in time");
    }
}
