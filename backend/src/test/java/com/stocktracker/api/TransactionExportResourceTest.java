package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class TransactionExportResourceTest extends IntegrationTestSupport {
  @Test
  void exportsTransactionsUsingCanonicalCsvHeaders() throws Exception {
    persistTransaction("2024-02-12", "MSFT", "buy", "3", "250.0000", "1.2500");

    var csv =
        given()
            .when()
            .get("/api/transactions/export")
            .then()
            .statusCode(200)
            .header("Content-Disposition", containsString("stocktracker-transactions.csv"))
            .extract()
            .asString();

    assertTrue(csv.startsWith("date,ticker,type,quantity,price,fees,amount,currency\n"));
    assertTrue(csv.contains("2024-02-12,MSFT,buy,3,250,1.25,,"));
  }
}
