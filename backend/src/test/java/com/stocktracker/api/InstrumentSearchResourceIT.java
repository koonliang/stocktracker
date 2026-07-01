package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.dto.AddInstrumentResponse;
import com.stocktracker.dto.InstrumentSearchResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class InstrumentSearchResourceIT extends IntegrationTestSupport {
  @Inject InstrumentRepository instrumentRepository;

  @Test
  void searchReturnsGlobalSgxMatch() {
    var response =
        given()
            .when()
            .get("/api/instruments/search?q=DBS")
            .then()
            .statusCode(200)
            .extract()
            .as(InstrumentSearchResponse.class);

    var match =
        response.results().stream().filter(r -> r.symbol().equalsIgnoreCase("D05.SI")).findFirst();
    assertTrue(match.isPresent(), "search should surface the SGX .SI example");
    assertEquals("SGD", match.get().currency());
  }

  @Test
  void addOnDemandCreatesInstrumentAndImmediateQuote() {
    var response =
        given()
            .contentType("application/json")
            .body("{\"symbol\":\"D05.SI\"}")
            .when()
            .post("/api/instruments")
            .then()
            .statusCode(201)
            .extract()
            .as(AddInstrumentResponse.class);

    assertEquals("D05.SI", response.symbol());
    assertEquals("SGD", response.currency());
    assertNotNull(response.quote());
    assertNotNull(response.quote().price());
    assertTrue(instrumentRepository.existsSymbol("D05.SI"));
  }

  @Test
  void unrecognizedSymbolIsRejectedWithNoRowCreated() {
    given()
        .contentType("application/json")
        .body("{\"symbol\":\"NOPE\"}")
        .when()
        .post("/api/instruments")
        .then()
        .statusCode(422);

    assertFalse(instrumentRepository.existsSymbol("NOPE"));
  }
}
