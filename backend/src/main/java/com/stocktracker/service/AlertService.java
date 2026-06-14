package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.domain.Alert;
import com.stocktracker.dto.AlertDtos.AlertListResponse;
import com.stocktracker.dto.AlertDtos.AlertRequest;
import com.stocktracker.dto.AlertDtos.AlertView;
import com.stocktracker.persistence.AlertRepository;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class AlertService {
  @Inject AlertRepository alerts;
  @Inject InstrumentRepository instruments;
  @Inject CurrentUser currentUser;

  public AlertListResponse list() {
    return new AlertListResponse(
        alerts.listForUser(currentUser.id()).stream().map(this::view).toList());
  }

  @Transactional
  public AlertView create(AlertRequest request) {
    var alert = new Alert();
    alert.userId = currentUser.id();
    apply(alert, request);
    alert.armed = true;
    alerts.persist(alert);
    return view(alert);
  }

  @Transactional
  public AlertView update(Long id, AlertRequest request) {
    var alert = owned(id);
    apply(alert, request);
    alert.armed = true;
    alerts.persist(alert);
    return view(alert);
  }

  @Transactional
  public void delete(Long id) {
    alerts.delete(owned(id));
  }

  private void apply(Alert alert, AlertRequest request) {
    if (request == null || request.symbol() == null || request.symbol().isBlank()) {
      throw new ApiException(
          ApiStatuses.UNPROCESSABLE_ENTITY, "validation_error", "symbol is required");
    }
    var symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
    if (!instruments.existsSymbol(symbol)) {
      throw new ApiException(
          ApiStatuses.UNPROCESSABLE_ENTITY, "validation_error", "symbol is unknown");
    }
    alert.instrumentSymbol = symbol;
    alert.conditionType = normalizeCondition(request.conditionType());
    if (request.threshold() == null || request.threshold().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ApiException(
          ApiStatuses.UNPROCESSABLE_ENTITY, "validation_error", "threshold must be positive");
    }
    alert.threshold = request.threshold();
  }

  private String normalizeCondition(String raw) {
    var value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    if (List.of("price_above", "price_below", "pct_change").contains(value)) {
      return value;
    }
    throw new ApiException(
        ApiStatuses.UNPROCESSABLE_ENTITY, "validation_error", "conditionType is invalid");
  }

  private Alert owned(Long id) {
    return alerts
        .findByIdAndUser(id, currentUser.id())
        .orElseThrow(() -> new ApiException(Status.NOT_FOUND, "not_found", "Alert not found"));
  }

  private AlertView view(Alert alert) {
    return new AlertView(
        alert.id.toString(),
        alert.instrumentSymbol,
        alert.conditionType,
        alert.threshold.doubleValue(),
        alert.armed,
        alert.lastTriggeredAt,
        alert.createdAt);
  }
}
