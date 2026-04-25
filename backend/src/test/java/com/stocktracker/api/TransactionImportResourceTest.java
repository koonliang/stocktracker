package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stocktracker.dto.DashboardResponse;
import com.stocktracker.dto.TransactionImportPreviewResponse;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class TransactionImportResourceTest extends IntegrationTestSupport {
  @Test
  void previewsMixedValidityCsvRows() {
    var csv =
        """
        date,ticker,type,quantity,price,fees
        2024-01-03,AAPL,buy,2,100,0
        2024-01-04,ZZZZ,buy,1,10,0
        """;

    var response =
        given()
            .multiPart("file", "transactions.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
            .when()
            .post("/api/transactions/import/preview")
            .then()
            .statusCode(200)
            .extract()
            .as(TransactionImportPreviewResponse.class);

    assertEquals(1, response.validRows().size());
    assertEquals(1, response.invalidRows().size());
    assertEquals("AAPL", response.validRows().getFirst().normalized().ticker());
  }

  @Test
  void commitsPreviewRowsIntoPortfolio() {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "rows",
                    List.of(
                        Map.of(
                            "date", "2024-01-03",
                            "ticker", "AAPL",
                            "type", "buy",
                            "quantity", 2,
                            "price", 100,
                            "fees", 0))))
            .when()
            .post("/api/transactions/import/commit")
            .then()
            .statusCode(200)
            .extract()
            .as(DashboardResponse.class);

    assertEquals(1, response.holdings().size());
    assertEquals("AAPL", response.holdings().getFirst().ticker());
  }
}
