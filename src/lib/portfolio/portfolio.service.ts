import Decimal from 'decimal.js';
import { holdingService } from '../holding/holding.service';
import { yahooFinanceService, HistoricalDataPoint } from '../external/yahoo-finance.service';
import { prisma } from '../database/prisma';
import { cacheService } from '../cache/cache.service';
import { Holding } from '../database/types';

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
  async getPortfolio(userId: number, refresh = false): Promise<PortfolioResponse> {
    const cacheKey = `portfolio:${userId}`;

    // Check cache if not refreshing
    if (!refresh) {
      const cached = await cacheService.get<PortfolioResponse>(cacheKey);
      if (cached) {
        console.log(`Portfolio cache hit for user ${userId}`);
        return cached;
      }
    }

    console.log(`Portfolio cache miss for user ${userId}`);

    // Get all holdings for the user
    const holdings = await holdingService.findByUserId(userId);

    if (holdings.length === 0) {
      const emptyResponse = {
        holdings: [],
        totalValue: 0,
        totalCost: 0,
        totalReturnDollars: 0,
        totalReturnPercent: 0,
        annualizedYield: 0,
        investmentYears: 0,
        pricesUpdatedAt: new Date().toISOString(),
      };
      // Cache empty response for 2 minutes
      await cacheService.set(cacheKey, emptyResponse, 120000);
      return emptyResponse;
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

    const response: PortfolioResponse = {
      holdings: holdingResponses,
      totalValue: totalPortfolioValue.toNumber(),
      totalCost: totalPortfolioCost.toNumber(),
      totalReturnDollars: totalReturnDollars.toNumber(),
      totalReturnPercent: totalReturnPercent.toNumber(),
      annualizedYield,
      investmentYears,
      pricesUpdatedAt: new Date().toISOString(),
    };

    // Cache for 2 minutes
    await cacheService.set(cacheKey, response, 120000);

    return response;
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
   * Calculate shares owned at a specific date based on transaction history
   */
  private async calculateSharesAtDate(
    userId: number,
    date: Date,
  ): Promise<Map<string, Decimal>> {
    // Fetch all transactions on or before this date
    const transactions = await prisma.transactions.findMany({
      where: {
        user_id: BigInt(userId),
        transaction_date: {
          lte: date,
        },
      },
      orderBy: { transaction_date: 'asc' },
    });

    const sharesMap = new Map<string, Decimal>();

    for (const tx of transactions) {
      const shares = new Decimal(tx.shares.toString());
      const currentShares = sharesMap.get(tx.symbol) || new Decimal(0);

      if (tx.type === 'BUY') {
        sharesMap.set(tx.symbol, currentShares.plus(shares));
      } else if (tx.type === 'SELL') {
        sharesMap.set(tx.symbol, currentShares.minus(shares));
      }
    }

    return sharesMap;
  }

  /**
   * Calculate the range string for "all" time based on earliest transaction
   */
  private async calculateAllTimeRange(userId: number): Promise<string> {
    const firstTransaction = await prisma.transactions.findFirst({
      where: { user_id: BigInt(userId) },
      orderBy: { transaction_date: 'asc' },
    });

    if (!firstTransaction) {
      return '1y'; // Default fallback
    }

    const firstDate = new Date(firstTransaction.transaction_date);
    const now = new Date();
    const daysBetween = Math.floor((now.getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));
    const years = daysBetween / 365;

    // Round up to nearest year and add buffer
    if (years < 1) return '1y';
    if (years < 2) return '2y';
    if (years < 5) return '5y';
    if (years < 10) return '10y';
    return 'max'; // Yahoo Finance supports "max" for all available data
  }

  /**
   * Get performance history for a given time range
   */
  async getPerformanceHistory(
    userId: number,
    range: '7d' | '1mo' | '3mo' | 'ytd' | '1y' | 'all' = '1y',
  ): Promise<PortfolioPerformancePoint[]> {
    const cacheKey = `performance:${userId}:${range}`;

    // Check cache
    const cached = await cacheService.get<PortfolioPerformancePoint[]>(cacheKey);
    if (cached) {
      console.log(`Performance history cache hit for user ${userId}, range ${range}`);
      return cached;
    }

    console.log(`Performance history cache miss for user ${userId}, range ${range}`);

    // Get all holdings
    const holdings = await holdingService.findByUserId(userId);

    if (holdings.length === 0) {
      const emptyResult: PortfolioPerformancePoint[] = [];
      // Cache empty result for 10 minutes
      await cacheService.set(cacheKey, emptyResult, 600000);
      return emptyResult;
    }

    // Get all symbols
    const symbols = holdings.map((h) => h.symbol);

    // For "all" range, calculate the range from earliest transaction
    let effectiveRange: string = range;
    if (range === 'all') {
      effectiveRange = await this.calculateAllTimeRange(userId);
    }

    // Fetch historical data for all symbols
    const historicalDataMap = await yahooFinanceService.getHistoricalDataBatch(
      symbols,
      effectiveRange,
    );

    // Get all transactions for this user
    const allTransactions = await prisma.transactions.findMany({
      where: { user_id: BigInt(userId) },
      orderBy: [{ symbol: 'asc' }, { transaction_date: 'asc' }],
    });

    if (allTransactions.length === 0) {
      console.warn(`No transaction history found for userId=${userId}. Using fallback calculation.`);
      // Fall back to using current holdings (less accurate)
      return this.aggregatePerformanceWithCurrentHoldings(holdings, historicalDataMap);
    }

    // Build a set of all unique dates from historical data (excluding today)
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const allDates = new Set<string>();

    for (const data of historicalDataMap.values()) {
      for (const price of data) {
        const priceDate = new Date(price.date);
        priceDate.setHours(0, 0, 0, 0);

        if (priceDate < today) {
          allDates.add(price.date);
        }
      }
    }

    // Sort dates
    const sortedDates = Array.from(allDates).sort();

    // For each date, calculate portfolio value based on shares owned at that time
    const dailyValues = new Map<string, Decimal>();

    for (const dateStr of sortedDates) {
      const date = new Date(dateStr);

      // Calculate shares owned at this date for each symbol
      const sharesAtDate = await this.calculateSharesAtDate(userId, date);

      // Calculate total portfolio value at this date
      let totalValue = new Decimal(0);

      for (const [symbol, shares] of sharesAtDate.entries()) {
        if (shares.lessThanOrEqualTo(0)) continue;

        // Get historical price for this symbol on this date
        const data = historicalDataMap.get(symbol);
        if (!data) continue;

        // Find the price for this specific date
        const priceData = data.find((p) => p.date === dateStr);
        if (priceData && priceData.close) {
          const priceAtDate = new Decimal(priceData.close);
          const holdingValue = shares.mul(priceAtDate);
          totalValue = totalValue.plus(holdingValue);
        }
      }

      if (totalValue.greaterThan(0)) {
        dailyValues.set(dateStr, totalValue);
      }
    }

    // Convert to PortfolioPerformancePoint list with daily changes
    const performance: PortfolioPerformancePoint[] = [];
    let previousValue: Decimal | null = null;

    for (const [date, currentValue] of dailyValues.entries()) {
      const dailyChange = previousValue ? currentValue.minus(previousValue) : new Decimal(0);
      const dailyChangePercent =
        previousValue && previousValue.greaterThan(0)
          ? dailyChange.div(previousValue).mul(100).toDecimalPlaces(4, Decimal.ROUND_HALF_UP)
          : new Decimal(0);

      performance.push({
        date,
        totalValue: currentValue.toDecimalPlaces(2, Decimal.ROUND_HALF_UP).toNumber(),
        dailyChange: dailyChange.toDecimalPlaces(2, Decimal.ROUND_HALF_UP).toNumber(),
        dailyChangePercent: dailyChangePercent.toNumber(),
      });

      previousValue = currentValue;
    }

    // Cache for 10 minutes
    await cacheService.set(cacheKey, performance, 600000);

    return performance;
  }

  /**
   * Fallback method: Use current holdings to calculate historical performance
   * (less accurate as it uses current shares for all dates)
   */
  private aggregatePerformanceWithCurrentHoldings(
    holdings: Holding[],
    historicalDataMap: Map<string, HistoricalDataPoint[]>,
  ): PortfolioPerformancePoint[] {
    // Build a map of date -> sum of (shares * close price)
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const dailyValues = new Map<string, Decimal>();

    for (const holding of holdings) {
      const data = historicalDataMap.get(holding.symbol);
      if (!data || data.length === 0) continue;

      const shares = new Decimal(holding.shares.toString());

      for (const price of data) {
        const priceDate = new Date(price.date);
        priceDate.setHours(0, 0, 0, 0);

        // Skip today's date as historical prices may be incomplete
        if (priceDate < today && price.close) {
          const holdingValue = shares.mul(new Decimal(price.close));
          const currentValue = dailyValues.get(price.date) || new Decimal(0);
          dailyValues.set(price.date, currentValue.plus(holdingValue));
        }
      }
    }

    // Convert to PortfolioPerformancePoint list with daily changes
    const performance: PortfolioPerformancePoint[] = [];
    let previousValue: Decimal | null = null;

    const sortedEntries = Array.from(dailyValues.entries()).sort((a, b) => a[0].localeCompare(b[0]));

    for (const [date, currentValue] of sortedEntries) {
      const dailyChange = previousValue ? currentValue.minus(previousValue) : new Decimal(0);
      const dailyChangePercent =
        previousValue && previousValue.greaterThan(0)
          ? dailyChange.div(previousValue).mul(100).toDecimalPlaces(4, Decimal.ROUND_HALF_UP)
          : new Decimal(0);

      performance.push({
        date,
        totalValue: currentValue.toDecimalPlaces(2, Decimal.ROUND_HALF_UP).toNumber(),
        dailyChange: dailyChange.toDecimalPlaces(2, Decimal.ROUND_HALF_UP).toNumber(),
        dailyChangePercent: dailyChangePercent.toNumber(),
      });

      previousValue = currentValue;
    }

    return performance;
  }
}

// Singleton instance
export const portfolioService = new PortfolioService();
