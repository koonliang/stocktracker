package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import com.stocktracker.domain.Alert;
import com.stocktracker.domain.Notification;
import com.stocktracker.dto.NotificationDtos.ReadAllRequest;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class NotificationsResourceTest extends IntegrationTestSupport {

  @BeforeEach
  void setUp() throws Exception {
    inTransaction(
        () -> {
          Notification.deleteAll();
          Alert.deleteAll();
        });
  }

  @Test
  void listReturnsEmptyWhenNoNotifications() {
    given()
        .when()
        .get("/api/notifications")
        .then()
        .statusCode(200)
        .body("notifications", hasSize(0))
        .body("unreadCount", equalTo(0));
  }

  @Test
  void listReturnsNotificationsNewestFirst() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "300", true);
    persistNotification(alertId, "AAPL", "price_above", "300", "310", false);
    Thread.sleep(50);
    persistNotification(alertId, "AAPL", "price_above", "300", "320", true);

    given()
        .when()
        .get("/api/notifications")
        .then()
        .statusCode(200)
        .body("notifications", hasSize(2))
        .body("unreadCount", equalTo(1));
  }

  @Test
  void listFiltersByUnread() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "300", true);
    persistNotification(alertId, "AAPL", "price_above", "300", "310", false);
    persistNotification(alertId, "AAPL", "price_above", "300", "320", true);

    given()
        .queryParam("unread", "true")
        .when()
        .get("/api/notifications")
        .then()
        .statusCode(200)
        .body("notifications", hasSize(1))
        .body("notifications[0].read", equalTo(false));
  }

  @Test
  void listRespectsLimit() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "300", true);
    persistNotification(alertId, "AAPL", "price_above", "300", "310", false);
    persistNotification(alertId, "AAPL", "price_above", "300", "320", false);
    persistNotification(alertId, "AAPL", "price_above", "300", "330", true);

    given()
        .queryParam("limit", "2")
        .when()
        .get("/api/notifications")
        .then()
        .statusCode(200)
        .body("notifications", hasSize(2))
        .body("nextCursor", notNullValue());
  }

  @Test
  void listEnforcesOwnership() throws Exception {
    // Notification for a different user should not appear
    given()
        .when()
        .get("/api/notifications")
        .then()
        .statusCode(200)
        .body("notifications", hasSize(0));
  }

  @Test
  void markReadReturns204() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "300", true);
    persistNotification(alertId, "AAPL", "price_above", "300", "310", false);

    var id = ((Notification) Notification.findAll().firstResult()).id.toString();

    given()
        .when()
        .post("/api/notifications/{id}/read", id)
        .then()
        .statusCode(204);

    given()
        .when()
        .get("/api/notifications")
        .then()
        .body("unreadCount", equalTo(0));
  }

  @Test
  void markReadReturns404ForUnknownId() {
    given()
        .when()
        .post("/api/notifications/{id}/read", 99999)
        .then()
        .statusCode(404);
  }

  @Test
  void markReadAllWithoutIdsMarksEverythingRead() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "300", true);
    persistNotification(alertId, "AAPL", "price_above", "300", "310", false);
    persistNotification(alertId, "AAPL", "price_above", "300", "320", false);

    given()
        .contentType(ContentType.JSON)
        .body(new ReadAllRequest(null))
        .when()
        .post("/api/notifications/read-all")
        .then()
        .statusCode(200)
        .body("updated", greaterThanOrEqualTo(2))
        .body("unreadCount", equalTo(0));

    given()
        .when()
        .get("/api/notifications")
        .then()
        .body("unreadCount", equalTo(0));
  }

  @Test
  void deleteRemovesNotification() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "300", true);
    persistNotification(alertId, "AAPL", "price_above", "300", "310", false);
    persistNotification(alertId, "AAPL", "price_above", "300", "320", true);

    var notification = (Notification) Notification.find("read = ?1", false).firstResult();
    var id = notification.id.toString();

    given()
        .when()
        .delete("/api/notifications/{id}", id)
        .then()
        .statusCode(204);

    given()
        .when()
        .get("/api/notifications")
        .then()
        .body("notifications", hasSize(1));
  }

  @Test
  void deleteReturns404ForUnknownId() {
    given()
        .when()
        .delete("/api/notifications/{id}", 99999)
        .then()
        .statusCode(404);
  }
}
