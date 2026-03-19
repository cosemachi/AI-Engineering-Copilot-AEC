package com.aec.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ObservabilityResourceTest {

    @Test
    void shouldExposeHealthEndpoint() {
        given()
                .when()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("aec-readiness"));
    }

    @Test
    void shouldExposePrometheusMetrics() {
        given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("jvm_info"));
    }
}
