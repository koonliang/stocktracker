import Decimal from 'decimal.js';
import { holdingService } from '../holding/holding.service';
import { yahooFinanceService } from '../external/yahoo-finance.service';
import { prisma } from '../database/prisma';

// Configure Decimal for financial calculations
Decimal.set({ rounding: Decimal.ROUND_HALF_UP, precision: 20 });

export interface HoldingResponse {
  id: number;
  symbol: string;
  companyName: string;
  shares: number;
  averageCost: number;
  lastPrice: number;
  previousClose: number | null;
  totalReturnDollars: number;
  totalReturnPercent: number;
  currentValue: number;
  costBasis: number;
  sevenDayReturnPercent: number;
  sevenDayReturnDollars: number;
  weight: number;
  sparklineData: number[];
}

export interface PortfolioResponse {
  holdings: HoldingResponse[];
  totalValue: number;
  totalCost: number;
  totalReturnDollars: number;
  totalReturnPercent: number;
  annualizedYield: number;
  investmentYears: number;
  pricesUpdatedAt: string;
}

export interface PortfolioPerformancePoint {
  date: string;
  totalValue: number;
  dailyChange: number;
  dailyChangePercent: number;
}

export class PortfolioService {
  /**
   * Get portfolio with current market prices
   */
  async getPortfolio(userId: number, _refresh = false): Promise<PortfolioResponse> {
    // Get all holdings for the user
    const holdings = await holdingService.findByUserId(userId);

    if (holdings.length === 0) {
      return {
        holdings: [],
        totalValue: 0,
        totalCost: 0,
        totalReturnDollars: 0,
        totalReturnPercent: 0,
        annualizedYield: 0,
        investmentYears: 0,
        pricesUpdatedAt: new Date().toISOString(),
      };
    }

    // Fetch current quotes and historical data
    const symbols = holdings.map((h) => h.symbol);
    const [quotes, historicalDataMap] = await Promise.all([
      yahooFinanceService.getQuotes(symbols),
      yahooFinanceService.getHistoricalDataBatch(symbols, '7d'),
    ]);

    // Calculate holding metrics
    const holdingResponses: HoldingResponse[] = [];
    let totalPortfolioValue = new Decimal(0);
    let totalPortfolioCost = new Decimal(0);

    for (const holding of holdings) {
      const quote = quotes.get(holding.symbol);
      const historicalData = historicalDataMap.get(holding.symbol) || [];

      const shares = new Decimal(holding.shares.toString());
      const avgCost = new Decimal(holding.average_cost.toString());
      const lastPrice = quote?.price ? new Decimal(quote.price) : avgCost;

      // Current value and cost basis
      const currentValue = lastPrice.mul(shares).toDecimalPlaces(2, Decimal.ROUND_HALF_UP);
      const costBasis = avgCost.mul(shares).toDecimalPlaces(2, Decimal.ROUND_HALF_UP);

      // Total return
      const totalReturnDollars = currentValue.minus(costBasis);
      const totalReturnPercent = costBasis.isZero()
        ? new Decimal(0)
        : totalReturnDollars.div(costBasis).mul(100).toDecimalPlaces(4, Decimal.ROUND_HALF_UP);

      // 7-day return
      let sevenDayReturnPercent = new Decimal(0);
      let sevenDayReturnDollars = new Decimal(0);
      if (historicalData.length > 0 && quote?.price) {
        const price7DaysAgo = new Decimal(historicalData[0].close);
        const change = lastPrice.minus(price7DaysAgo);
        sevenDayReturnPercent = price7DaysAgo.isZero()
          ? new Decimal(0)
          : change.div(price7DaysAgo).mul(100).toDecimalPlaces(4, Decimal.ROUND_HALF_UP);
        sevenDayReturnDollars = change.mul(shares).toDecimalPlaces(2, Decimal.ROUND_HALF_UP);
      }

      // Sparkline data (downsample to ~52 points)
      const sparklineData = this.downsampleSparkline(
        historicalData.map((d) => d.close),
        52,
      );

      holdingResponses.push({
        id: Number(holding.id),
        symbol: holding.symbol,
        companyName: holding.company_name,
        shares: shares.toNumber(),
        averageCost: avgCost.toNumber(),
        lastPrice: lastPrice.toNumber(),
        previousClose: quote?.previousClose ?? null,
        totalReturnDollars: totalReturnDollars.toNumber(),
        totalReturnPercent: totalReturnPercent.toNumber(),
        currentValue: currentValue.toNumber(),
        costBasis: costBasis.toNumber(),
        sevenDayReturnPercent: sevenDayReturnPercent.toNumber(),
        sevenDayReturnDollars: sevenDayReturnDollars.toNumber(),
        weight: 0, // Will calculate after totals
        sparklineData,
      });

      totalPortfolioValue = totalPortfolioValue.plus(currentValue);
      totalPortfolioCost = totalPortfolioCost.plus(costBasis);
    }

    // Calculate weights
    holdingResponses.forEach((h) => {
      h.weight = totalPortfolioValue.isZero()
        ? 0
        : new Decimal(h.currentValue)
            .div(totalPortfolioValue)
            .mul(100)
            .toDecimalPlaces(4, Decimal.ROUND_HALF_UP)
            .toNumber();
    });

    // Calculate portfolio totals
    const totalReturnDollars = totalPortfolioValue.minus(totalPortfolioCost);
    const totalReturnPercent = totalPortfolioCost.isZero()
      ? new Decimal(0)
      : totalReturnDollars
          .div(totalPortfolioCost)
          .mul(100)
          .toDecimalPlaces(4, Decimal.ROUND_HALF_UP);

    // Calculate annualized yield (CAGR)
    const { annualizedYield, investmentYears } = await this.calculateAnnualizedYield(
      userId,
      totalReturnPercent,
    );

    return {
      holdings: holdingResponses,
      totalValue: totalPortfolioValue.toNumber(),
      totalCost: totalPortfolioCost.toNumber(),
      totalReturnDollars: totalReturnDollars.toNumber(),
      totalReturnPercent: totalReturnPercent.toNumber(),
      annualizedYield,
      investmentYears,
      pricesUpdatedAt: new Date().toISOString(),
    };
  }

  /**
   * Calculate annualized yield (CAGR)
   */
  private async calculateAnnualizedYield(
    userId: number,
    totalReturnPercent: Decimal,
  ): Promise<{ annualizedYield: number; investmentYears: number }> {
    // Get first transaction date
    const firstTransaction = await prisma.transactions.findFirst({
      where: { user_id: BigInt(userId) },
      orderBy: { transaction_date: 'asc' },
    });

    if (!firstTransaction) {
      return { annualizedYield: 0, investmentYears: 0 };
    }

    const firstDate = new Date(firstTransaction.transaction_date);
    const now = new Date();
    const daysBetween = Math.floor((now.getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));
    const years = daysBetween / 365.25;

    // If less than 36 days, return simple return percent
    if (years < 0.1) {
      return {
        annualizedYield: totalReturnPercent.toDecimalPlaces(2, Decimal.ROUND_HALF_UP).toNumber(),
        investmentYears: years,
      };
    }

    // Calculate CAGR: ((1 + totalReturn) ^ (1/years)) - 1
    const totalReturnDecimal = totalReturnPercent.div(100);
    const base = totalReturnDecimal.plus(1);
    const exponent = 1 / years;
    const annualized = Math.pow(base.toNumber(), exponent) - 1;
    const annualizedPercent = new Decimal(annualized)
      .mul(100)
      .toDecimalPlaces(2, Decimal.ROUND_HALF_UP);

    return {
      annualizedYield: annualizedPercent.toNumber(),
      investmentYears: years,
    };
  }

  /**
   * Downsample array to target size
   */
  private downsampleSparkline(data: number[], targetSize: number): number[] {
    if (data.length <= targetSize) {
      return data;
    }

    const step = Math.max(1, Math.floor(data.length / targetSize));
    const result: number[] = [];

    for (let i = 0; i < data.length; i += step) {
      result.push(data[i]);
    }

    return result;
  }

  /**
   * Get performance history for a given time range
   */
  async getPerformanceHistory(
    _userId: number,
    _range: '7d' | '1mo' | '3mo' | 'ytd' | '1y' | 'all' = '1y',
  ): Promise<PortfolioPerformancePoint[]> {
    // For now, return empty array
    // Full implementation requires transaction-based historical calculation
    return [];
  }
}

// Singleton instance
export const portfolioService = new PortfolioService();
