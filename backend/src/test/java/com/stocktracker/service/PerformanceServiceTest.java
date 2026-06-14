package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PerformanceServiceTest {
  @Test
  void timeWeightedReturnChainsSubPeriodReturns() {
    var factor = BigDecimal.ONE;
    factor =
        PerformanceService.nextTwrFactor(
            factor, new BigDecimal("100.00"), new BigDecimal("110.00"));
    factor =
        PerformanceService.nextTwrFactor(factor, new BigDecimal("110.00"), new BigDecimal("99.00"));

    assertEquals(
        0, PerformanceService.factorToPercent(factor).compareTo(new BigDecimal("-1.000000000000")));
  }

  @Test
  void contributionsReconcileToTotalWithinTolerance() {
    var first = PerformanceService.contributionPct(new BigDecimal("30"), new BigDecimal("100"));
    var second = PerformanceService.contributionPct(new BigDecimal("70"), new BigDecimal("100"));

    assertEquals(0, first.add(second).compareTo(new BigDecimal("100.00000000")));
  }
}
