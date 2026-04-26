package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.dto.TransactionRequest;
import com.stocktracker.persistence.InstrumentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TransactionValidationService {
  @Inject InstrumentRepository instrumentRepository;

  public TransactionRequest normalize(TransactionRequest request) {
    return new TransactionRequest(
        request.date(),
        request.ticker().trim().toUpperCase(),
        request.type().trim().toLowerCase(),
        request.quantity().stripTrailingZeros(),
        request.price().stripTrailingZeros(),
        request.fees().stripTrailingZeros());
  }

  public void validateBatch(
      List<TransactionRequest> requests, Map<String, BigDecimal> shareBalances) {
    for (var request : requests) {
      var normalized = normalize(request);
      var issue = validate(normalized, shareBalances);
      if (issue != null) {
        throw new ApiException(ApiStatuses.UNPROCESSABLE_ENTITY, "validation_error", issue);
      }
      applyToBalances(normalized, shareBalances);
    }
  }

  public String validate(TransactionRequest request, Map<String, BigDecimal> shareBalances) {
    if (request.date() == null || request.date().isAfter(LocalDate.now())) {
      return "date is in the future";
    }
    if (!instrumentRepository.existsSymbol(request.ticker())) {
      return "unknown ticker: " + request.ticker();
    }
    if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
      return "quantity must be > 0";
    }
    if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
      return "price must be > 0";
    }
    if (request.fees() == null || request.fees().compareTo(BigDecimal.ZERO) < 0) {
      return "fees must be >= 0";
    }
    if (!"buy".equals(request.type()) && !"sell".equals(request.type())) {
      return "type must be buy or sell";
    }
    if ("sell".equals(request.type())) {
      var available = shareBalances.getOrDefault(request.ticker(), BigDecimal.ZERO);
      if (available.compareTo(request.quantity()) < 0) {
        return "sell quantity exceeds held shares";
      }
    }
    return null;
  }

  public void applyToBalances(TransactionRequest request, Map<String, BigDecimal> shareBalances) {
    var current = shareBalances.getOrDefault(request.ticker(), BigDecimal.ZERO);
    if ("buy".equals(request.type())) {
      shareBalances.put(request.ticker(), current.add(request.quantity()));
    } else {
      shareBalances.put(request.ticker(), current.subtract(request.quantity()));
    }
  }

  public Map<String, Object> fieldDetail(String field, Object value) {
    var details = new LinkedHashMap<String, Object>();
    details.put("field", field);
    details.put("value", value);
    return details;
  }
}
