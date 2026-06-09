package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.dto.DashboardResponse;
import com.stocktracker.dto.TransactionResponse;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.common.mapper.TypeRef;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class TransactionsResourceTest extends IntegrationTestSupport {
  @Test
  void listsTransactionsInDescendingTradeDateOrder() throws Exception {
    persistTransaction("2024-01-10", "AAPL", "buy", "2", "100.0000", "0.0000");
    persistTransaction("2024-02-10", "MSFT", "buy", "3", "200.0000", "0.0000");

    List<TransactionResponse> response =
        given()
            .when()
            .get("/api/transactions")
            .then()
            .statusCode(200)
            .extract()
            .as(new TypeRef<>() {});

    assertEquals(2, response.size());
    assertEquals("MSFT", response.getFirst().ticker());
    assertEquals("AAPL", response.getLast().ticker());
  }

  @Test
  void deletesTransactionsAndReturnsUpdatedDashboard() throws Exception {
    var transactionId = persistTransaction("2024-02-10", "AAPL", "buy", "2", "100.0000", "0.0000");

    var response =
        given()
            .when()
            .delete("/api/transactions/{transactionId}", transactionId)
            .then()
            .statusCode(200)
            .extract()
            .as(DashboardResponse.class);

    assertTrue(response.holdings().isEmpty());
  }
}
