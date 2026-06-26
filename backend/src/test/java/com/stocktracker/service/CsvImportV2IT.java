package com.stocktracker.service;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.dto.TransactionImportPreviewResponse;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class CsvImportV2IT extends IntegrationTestSupport {
  @Test
  void importsV1WithoutAmountCurrencyColumns() {
    var csv =
        """
        date,ticker,type,quantity,price,fees
        2024-01-03,AAPL,buy,2,100,0
        """;

    var response = preview(csv);

    assertEquals("v1", response.detectedVersion());
    assertEquals(1, response.validRows().size());
    assertEquals("AAPL", response.validRows().getFirst().normalized().ticker());
    assertEquals(null, response.validRows().getFirst().normalized().amount());
  }

  @Test
  void importsV2NewTypesAndExportsV2Header() throws Exception {
    var csv =
        """
        date,ticker,type,quantity,price,fees,amount,currency
        2024-01-03,AAPL,buy,2,100,0,,
        2024-01-04,AAPL,dividend,,,0,5.25,
        2024-01-05,AAPL,split,2,,,,
        2024-01-06,,deposit,,,,1000,USD
        """;

    var preview = preview(csv);

    assertEquals("v2", preview.detectedVersion());
    assertEquals(4, preview.validRows().size());
    assertEquals("deposit", preview.validRows().get(3).normalized().type());
    assertEquals("USD", preview.validRows().get(3).normalized().currency());

    given()
        .contentType("application/json")
        .body(Map.of("rows", preview.validRows().stream().map(row -> row.normalized()).toList()))
        .when()
        .post("/api/transactions/import/commit")
        .then()
        .statusCode(200);

    var exported =
        given()
            .when()
            .get("/api/transactions/export")
            .then()
            .statusCode(200)
            .header("Content-Disposition", containsString("stocktracker-transactions.csv"))
            .extract()
            .asString();

    assertTrue(exported.startsWith("date,ticker,type,quantity,price,fees,amount,currency\n"));
    assertTrue(exported.contains("2024-01-06,,deposit,0,0,0,1000,USD"));
  }

  private TransactionImportPreviewResponse preview(String csv) {
    return given()
        .multiPart("file", "transactions.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
        .when()
        .post("/api/transactions/import/preview")
        .then()
        .statusCode(200)
        .extract()
        .as(TransactionImportPreviewResponse.class);
  }
}
