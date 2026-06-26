package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.Instrument;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class AlertsResourceIT extends IntegrationTestSupport {
  @Test
  void crudAlertsAndRearmOnEdit() {
    var id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"symbol\":\"AAPL\",\"conditionType\":\"price_above\",\"threshold\":200}")
            .when()
            .post("/api/alerts")
            .then()
            .statusCode(201)
            .body("symbol", equalTo("AAPL"))
            .extract()
            .path("id");

    given().when().get("/api/alerts").then().statusCode(200).body("alerts.size()", equalTo(1));

    given()
        .contentType(ContentType.JSON)
        .body("{\"symbol\":\"AAPL\",\"conditionType\":\"price_below\",\"threshold\":150}")
        .when()
        .patch("/api/alerts/" + id)
        .then()
        .statusCode(200)
        .body("armed", equalTo(true))
        .body("conditionType", equalTo("price_below"));

    given().when().delete("/api/alerts/" + id).then().statusCode(204);
    given().when().get("/api/alerts").then().statusCode(200).body("alerts.size()", equalTo(0));
  }

  @Test
  void creatingAlertAutoAddsRecognizedUnknownSymbol() throws Exception {
    given()
        .contentType(ContentType.JSON)
        .body("{\"symbol\":\"D05.SI\",\"conditionType\":\"price_above\",\"threshold\":20}")
        .when()
        .post("/api/alerts")
        .then()
        .statusCode(201)
        .body("symbol", equalTo("D05.SI"));

    inTransaction(() -> assertTrue(Instrument.<Instrument>findByIdOptional("D05.SI").isPresent()));
  }
}
