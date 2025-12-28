package com.stocktracker.service;

import com.stocktracker.client.YahooFinanceClient;
import com.stocktracker.client.dto.StockQuote;
import com.stocktracker.dto.response.HoldingResponse;
import com.stocktracker.dto.response.PortfolioResponse;
import com.stocktracker.entity.Holding;
import com.stocktracker.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final YahooFinanceClient yahooFinanceClient;

    /**
     * Get user's portfolio with live prices and calculated returns.
     * Results are cached for 2 minutes to reduce Yahoo Finance API calls.
     */
    @Cacheable(value = "portfolio", key = "#userId", unless = "#result == null")
    public PortfolioResponse getPortfolio(Long userId) {
        log.debug("Fetching portfolio for user: {}", userId);

        // 1. Fetch holdings from database
        List<Holding> holdings = holdingRepository.findByUserIdOrderBySymbolAsc(userId);

        if (holdings.isEmpty()) {
            log.debug("No holdings found for user: {}", userId);
            return PortfolioResponse.builder()
                    .holdings(Collections.emptyList())
                    .totalValue(BigDecimal.ZERO)
                    .totalCost(BigDecimal.ZERO)
                    .totalReturnDollars(BigDecimal.ZERO)
                    .totalReturnPercent(BigDecimal.ZERO)
                    .pricesUpdatedAt(LocalDateTime.now())
                    .build();
        }

        // 2. Fetch live prices from Yahoo Finance
        List<String> symbols = holdings.stream()
                .map(Holding::getSymbol)
                .collect(Collectors.toList());

        log.debug("Fetching live prices for {} symbols", symbols.size());
        Map<String, StockQuote> quotes = yahooFinanceClient.getQuotes(symbols);

        // 3. Calculate returns for each holding
        List<HoldingResponse> holdingResponses = holdings.stream()
                .map(h -> buildHoldingResponse(h, quotes.get(h.getSymbol())))
                .collect(Collectors.toList());

        // 4. Calculate portfolio totals
        PortfolioResponse response = buildPortfolioResponse(holdingResponses);

        log.debug("Portfolio calculated for user {}: Total Value = {}, Total Return = {}%",
                userId, response.getTotalValue(), response.getTotalReturnPercent());

        return response;
    }

    /**
     * Build a HoldingResponse with calculated values.
     */
    private HoldingResponse buildHoldingResponse(Holding holding, StockQuote quote) {
        BigDecimal lastPrice = quote != null && quote.getRegularMarketPrice() != null
                ? quote.getRegularMarketPrice()
                : BigDecimal.ZERO;

        BigDecimal shares = holding.getShares();
        BigDecimal avgCost = holding.getAverageCost();

        // Calculate current value and cost basis
        BigDecimal currentValue = lastPrice.multiply(shares);
        BigDecimal costBasis = avgCost.multiply(shares);

        // Calculate returns
        BigDecimal returnDollars = currentValue.subtract(costBasis);
        BigDecimal returnPercent = BigDecimal.ZERO;

        if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
            returnPercent = returnDollars
                    .divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return HoldingResponse.builder()
                .id(holding.getId())
                .symbol(holding.getSymbol())
                .companyName(holding.getCompanyName())
                .shares(shares)
                .averageCost(avgCost)
                .lastPrice(lastPrice)
                .previousClose(quote != null ? quote.getRegularMarketPreviousClose() : null)
                .currentValue(currentValue)
                .costBasis(costBasis)
                .totalReturnDollars(returnDollars)
                .totalReturnPercent(returnPercent)
                .build();
    }

    /**
     * Build a PortfolioResponse with aggregated totals.
     */
    private PortfolioResponse buildPortfolioResponse(List<HoldingResponse> holdings) {
        BigDecimal totalValue = holdings.stream()
                .map(HoldingResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = holdings.stream()
                .map(HoldingResponse::getCostBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturnDollars = totalValue.subtract(totalCost);
        BigDecimal totalReturnPercent = BigDecimal.ZERO;

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnPercent = totalReturnDollars
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return PortfolioResponse.builder()
                .holdings(holdings)
                .totalValue(totalValue)
                .totalCost(totalCost)
                .totalReturnDollars(totalReturnDollars)
                .totalReturnPercent(totalReturnPercent)
                .pricesUpdatedAt(LocalDateTime.now())
                .build();
    }
}
