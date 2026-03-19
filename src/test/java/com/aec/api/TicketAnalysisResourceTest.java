package com.aec.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TicketAnalysisResourceTest {

    @Test
    void shouldAnalyzeJsonTicket() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "source": "json",
                          "identifier": "examples/tickets/sample-ticket.json"
                        }
                        """)
                .when()
                .post("/ticket/analyze")
                .then()
                .statusCode(200)
                .body("summary", containsString("Add cache invalidation to deployment flow"));
    }
}
