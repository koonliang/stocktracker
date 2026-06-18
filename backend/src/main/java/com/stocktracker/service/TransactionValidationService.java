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
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TransactionValidationService {
  /** Types that reference an instrument and contribute to a position. */
  private static final Set<String> SECURITY_TYPES = Set.of("buy", "sell", "dividend", "split");

  /** Cash-movement types: no symbol, carry amount + currency. */
  private static final Set<String> CASH_TYPES = Set.of("deposit", "withdrawal", "fee");

  @Inject InstrumentRepository instrumentRepository;

  @ConfigProperty(name = "stocktracker.base-currency.default", defaultValue = "USD")
  String defaultBaseCurrency;

  public TransactionRequest normalize(TransactionRequest request) {
    return new TransactionRequest(
        request.date(),
        request.ticker() == null || request.ticker().isBlank()
            ? null
            : request.ticker().trim().toUpperCase(),
        request.type() == null ? null : request.type().trim().toLowerCase(),
        request.quantity() == null ? null : request.quantity().stripTrailingZeros(),
        request.price() == null ? null : request.price().stripTrailingZeros(),
        request.fees() == null ? BigDecimal.ZERO : request.fees().stripTrailingZeros(),
        request.amount() == null ? null : request.amount().stripTrailingZeros(),
        request.currency() == null || request.currency().isBlank()
            ? null
            : request.currency().trim().toUpperCase());
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
    var type = request.type();
    if (!SECURITY_TYPES.contains(type) && !CASH_TYPES.contains(type)) {
      return "unknown transaction type: " + type;
    }

    if (SECURITY_TYPES.contains(type)) {
      if (request.ticker() == null) {
        return type + " requires a ticker";
      }
      var instrument = instrumentRepository.findBySymbol(request.ticker()).orElse(null);
      if (instrument == null) {
        return "unknown ticker: " + request.ticker();
      }
      var currencyIssue = validateSecurityCurrency(request, instrument.currency);
      if (currencyIssue != null) {
        return currencyIssue;
      }
    } else {
      if (request.ticker() != null) {
        return type + " must not reference a ticker";
      }
      if (request.currency() == null) {
        return type + " requires a currency";
      }
      if (!supportedCurrency(request.currency())) {
        return "unsupported currency: " + request.currency();
      }
    }

    return switch (type) {
      case "buy", "sell" -> validateTrade(request, shareBalances);
      case "split" -> positiveQuantity(request, "split ratio");
      case "dividend", "deposit", "withdrawal", "fee" -> positiveAmount(request);
      default -> null;
    };
  }

  private String validateTrade(TransactionRequest request, Map<String, BigDecimal> shareBalances) {
    if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
      return "quantity must be > 0";
    }
    if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
      return "price must be > 0";
    }
    if (request.fees() != null && request.fees().compareTo(BigDecimal.ZERO) < 0) {
      return "fees must be >= 0";
    }
    if ("sell".equals(request.type())) {
      var available = shareBalances.getOrDefault(request.ticker(), BigDecimal.ZERO);
      if (available.compareTo(request.quantity()) < 0) {
        return "sell quantity exceeds held shares";
      }
    }
    return null;
  }

  private String positiveQuantity(TransactionRequest request, String label) {
    if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
      return label + " must be > 0";
    }
    return null;
  }

  private String positiveAmount(TransactionRequest request) {
    if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
      return "amount must be > 0";
    }
    return null;
  }

  /** A security txn's currency, if provided, must match the instrument's native currency. */
  private String validateSecurityCurrency(TransactionRequest request, String instrumentCurrency) {
    if (request.currency() != null && !instrumentCurrency.equalsIgnoreCase(request.currency())) {
      return "currency must match the instrument currency (" + instrumentCurrency + ")";
    }
    if (!supportedCurrency(request.currency() == null ? instrumentCurrency : request.currency())) {
      return "unsupported currency: " + (request.currency() == null ? instrumentCurrency : request.currency());
    }
    return null;
  }

  private boolean supportedCurrency(String currency) {
    return currency != null && List.of(defaultBaseCurrency.toUpperCase(), "USD", "SGD", "EUR").contains(currency);
  }

  public void applyToBalances(TransactionRequest request, Map<String, BigDecimal> shareBalances) {
    var type = request.type();
    if (request.ticker() == null) {
      return;
    }
    var current = shareBalances.getOrDefault(request.ticker(), BigDecimal.ZERO);
    switch (type) {
      case "buy" -> shareBalances.put(request.ticker(), current.add(request.quantity()));
      case "sell" -> shareBalances.put(request.ticker(), current.subtract(request.quantity()));
      case "split" -> shareBalances.put(request.ticker(), current.multiply(request.quantity()));
      default -> {
        // dividend and cash types do not change the share balance
      }
    }
  }

  public Map<String, Object> fieldDetail(String field, Object value) {
    var details = new LinkedHashMap<String, Object>();
    details.put("field", field);
    details.put("value", value);
    return details;
  }
}
