import axios from 'axios';

export interface StockQuote {
  symbol: string;
  price: number | null;
  previousClose: number | null;
  shortName?: string | null;
  regularMarketPrice?: number | null;
}

export interface HistoricalDataPoint {
  date: string; // ISO date string
  close: number;
}

export class YahooFinanceService {
  private baseUrl: string;
  private timeout = {
    connect: 5000,
    read: 10000,
  };

  constructor() {
    this.baseUrl =
      process.env.YAHOO_FINANCE_CHART_URL ||
      'https://query1.finance.yahoo.com/v8/finance/chart';
  }

  /**
   * Get current quote for a single symbol
   */
  async getQuote(symbol: string): Promise<StockQuote> {
    try {
      const response = await axios.get(`${this.baseUrl}/${symbol}`, {
        timeout: this.timeout.read,
        params: {
          interval: '1d',
          range: '1d',
        },
      });

      const result = response.data?.chart?.result?.[0];
      if (!result) {
        return {
          symbol,
          price: null,
          previousClose: null,
          shortName: null,
          regularMarketPrice: null,
        };
      }

      const meta = result.meta;
      const price = meta?.regularMarketPrice ?? null;
      const previousClose = meta?.previousClose ?? null;
      const shortName = meta?.shortName ?? meta?.longName ?? null;
      const regularMarketPrice = meta?.regularMarketPrice ?? null;

      return {
        symbol,
        price,
        previousClose,
        shortName,
        regularMarketPrice,
      };
    } catch (error) {
      console.error(`Failed to fetch quote for ${symbol}:`, error);
      return {
        symbol,
        price: null,
        previousClose: null,
        shortName: null,
        regularMarketPrice: null,
      };
    }
  }

  /**
   * Get current quotes for multiple symbols in parallel
   */
  async getQuotes(symbols: string[]): Promise<Map<string, StockQuote>> {
    const promises = symbols.map((symbol) => this.getQuote(symbol));
    const quotes = await Promise.all(promises);

    const quoteMap = new Map<string, StockQuote>();
    quotes.forEach((quote) => {
      quoteMap.set(quote.symbol, quote);
    });

    return quoteMap;
  }

  /**
   * Get historical data for a single symbol
   */
  async getHistoricalData(
    symbol: string,
    range: string = '7d',
  ): Promise<HistoricalDataPoint[]> {
    try {
      const response = await axios.get(`${this.baseUrl}/${symbol}`, {
        timeout: this.timeout.read,
        params: {
          interval: '1d',
          range,
        },
      });

      const result = response.data?.chart?.result?.[0];
      if (!result) {
        return [];
      }

      const timestamps = result.timestamp || [];
      const closes = result.indicators?.quote?.[0]?.close || [];

      const data: HistoricalDataPoint[] = [];
      for (let i = 0; i < timestamps.length; i++) {
        const close = closes[i];
        if (close != null) {
          const date = new Date(timestamps[i] * 1000).toISOString().split('T')[0];
          data.push({ date, close });
        }
      }

      return data;
    } catch (error) {
      console.error(`Failed to fetch historical data for ${symbol}:`, error);
      return [];
    }
  }

  /**
   * Get historical data for multiple symbols in parallel
   */
  async getHistoricalDataBatch(
    symbols: string[],
    range: string = '7d',
  ): Promise<Map<string, HistoricalDataPoint[]>> {
    const promises = symbols.map((symbol) => this.getHistoricalData(symbol, range));
    const results = await Promise.all(promises);

    const dataMap = new Map<string, HistoricalDataPoint[]>();
    symbols.forEach((symbol, index) => {
      dataMap.set(symbol, results[index]);
    });

    return dataMap;
  }
}

// Singleton instance
export const yahooFinanceService = new YahooFinanceService();
