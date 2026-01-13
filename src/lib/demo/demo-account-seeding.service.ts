import { transactionService, TransactionCreateData } from '../transaction/transaction.service';
import { holdingRecalculationService } from '../holding/holding-recalculation.service';
import { TransactionType } from '../database/types';

export class DemoAccountSeedingService {
  /**
   * Seed demo account with sample transactions
   * Base date: 90 days ago from today
   */
  async seedDemoTransactions(userId: number): Promise<void> {
    const baseDate = new Date();
    baseDate.setDate(baseDate.getDate() - 90);

    const transactions: TransactionCreateData[] = [
      // AAPL: BUY 60 @ $142.50 (+0 days)
      {
        userId,
        type: TransactionType.BUY,
        symbol: 'AAPL',
        companyName: 'Apple Inc.',
        shares: 60,
        pricePerShare: 142.5,
        transactionDate: this.addDays(baseDate, 0),
      },
      // MSFT: BUY 30 @ $285.00 (+5 days)
      {
        userId,
        type: TransactionType.BUY,
        symbol: 'MSFT',
        companyName: 'Microsoft Corporation',
        shares: 30,
        pricePerShare: 285.0,
        transactionDate: this.addDays(baseDate, 5),
      },
      // GOOGL: BUY 10 @ $125.30 (+10 days)
      {
        userId,
        type: TransactionType.BUY,
        symbol: 'GOOGL',
        companyName: 'Alphabet Inc.',
        shares: 10,
        pricePerShare: 125.3,
        transactionDate: this.addDays(baseDate, 10),
      },
      // TSLA: BUY 20 @ $248.00 (+15 days)
      {
        userId,
        type: TransactionType.BUY,
        symbol: 'TSLA',
        companyName: 'Tesla, Inc.',
        shares: 20,
        pricePerShare: 248.0,
        transactionDate: this.addDays(baseDate, 15),
      },
      // NVDA: BUY 20 @ $450.00 (+20 days)
      {
        userId,
        type: TransactionType.BUY,
        symbol: 'NVDA',
        companyName: 'NVIDIA Corporation',
        shares: 20,
        pricePerShare: 450.0,
        transactionDate: this.addDays(baseDate, 20),
      },
      // AMZN: BUY 40 @ $135.00 (+25 days)
      {
        userId,
        type: TransactionType.BUY,
        symbol: 'AMZN',
        companyName: 'Amazon.com, Inc.',
        shares: 40,
        pricePerShare: 135.0,
        transactionDate: this.addDays(baseDate, 25),
      },
      // AAPL: SELL 10 @ $150.00 (+30 days)
      {
        userId,
        type: TransactionType.SELL,
        symbol: 'AAPL',
        companyName: 'Apple Inc.',
        shares: 10,
        pricePerShare: 150.0,
        transactionDate: this.addDays(baseDate, 30),
      },
      // MSFT: SELL 5 @ $320.00 (+35 days)
      {
        userId,
        type: TransactionType.SELL,
        symbol: 'MSFT',
        companyName: 'Microsoft Corporation',
        shares: 5,
        pricePerShare: 320.0,
        transactionDate: this.addDays(baseDate, 35),
      },
      // TSLA: SELL 5 @ $265.00 (+45 days)
      {
        userId,
        type: TransactionType.SELL,
        symbol: 'TSLA',
        companyName: 'Tesla, Inc.',
        shares: 5,
        pricePerShare: 265.0,
        transactionDate: this.addDays(baseDate, 45),
      },
      // AMZN: SELL 10 @ $145.00 (+55 days)
      {
        userId,
        type: TransactionType.SELL,
        symbol: 'AMZN',
        companyName: 'Amazon.com, Inc.',
        shares: 10,
        pricePerShare: 145.0,
        transactionDate: this.addDays(baseDate, 55),
      },
    ];

    // Create all transactions
    await transactionService.createMany(transactions);

    // Recalculate holdings for all symbols
    await holdingRecalculationService.recalculateAllHoldings(userId);
  }

  /**
   * Helper to add days to a date
   */
  private addDays(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  }
}

// Singleton instance
export const demoAccountSeedingService = new DemoAccountSeedingService();
