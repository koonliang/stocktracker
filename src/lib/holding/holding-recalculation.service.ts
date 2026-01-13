import Decimal from 'decimal.js';
import { transactionService } from '../transaction/transaction.service';
import { holdingService } from './holding.service';
import { TransactionType } from '../database/types';

// Configure Decimal for financial calculations
Decimal.set({ rounding: Decimal.ROUND_HALF_UP, precision: 20 });

export class HoldingRecalculationService {
  /**
   * Recalculate holdings for a specific symbol
   * Implements weighted average cost algorithm matching Java backend
   */
  async recalculateHolding(userId: number, symbol: string): Promise<void> {
    // Get all transactions for this user and symbol, ordered by date ASC
    const transactions = await transactionService.findByUserIdAndSymbol(userId, symbol);

    if (transactions.length === 0) {
      // No transactions, delete holding if exists
      try {
        await holdingService.delete(userId, symbol);
      } catch {
        // Holding might not exist, ignore error
      }
      return;
    }

    let totalCost = new Decimal(0);
    let totalShares = new Decimal(0);
    let companyName = transactions[0].company_name;

    // Process each transaction in chronological order
    for (const tx of transactions) {
      const shares = new Decimal(tx.shares.toString());
      const pricePerShare = new Decimal(tx.price_per_share.toString());

      if (tx.type === TransactionType.BUY) {
        // BUY: Add to total cost and shares
        const cost = shares.mul(pricePerShare);
        totalCost = totalCost.plus(cost);
        totalShares = totalShares.plus(shares);
      } else if (tx.type === TransactionType.SELL) {
        // SELL: Calculate average cost at time of sale
        if (totalShares.isZero()) {
          console.warn(`Warning: SELL transaction without prior BUY for ${symbol}`);
          continue;
        }

        // Average cost at time of sale (4 decimals)
        const avgCostAtSale = totalCost.div(totalShares).toDecimalPlaces(4, Decimal.ROUND_HALF_UP);

        // Cost reduction from selling
        const costReduction = shares.mul(avgCostAtSale);

        // Update totals
        totalCost = totalCost.minus(costReduction);
        totalShares = totalShares.minus(shares);
      }

      // Update company name if we have a more recent one
      companyName = tx.company_name;
    }

    // Check if we should delete or update the holding
    if (totalShares.lte(0)) {
      // No shares left, delete holding
      try {
        await holdingService.delete(userId, symbol);
      } catch {
        // Holding might not exist, ignore error
      }
      return;
    }

    // Calculate final average cost (2 decimals)
    const averageCost = totalCost
      .div(totalShares)
      .toDecimalPlaces(2, Decimal.ROUND_HALF_UP)
      .toNumber();

    // Create or update holding
    await holdingService.upsert(userId, symbol, {
      companyName,
      shares: totalShares.toNumber(),
      averageCost,
    });
  }

  /**
   * Recalculate all holdings for a user
   */
  async recalculateAllHoldings(userId: number): Promise<void> {
    // Get all transactions for user
    const transactions = await transactionService.findByUserId(userId);

    // Get unique symbols
    const symbols = [...new Set(transactions.map((tx) => tx.symbol))];

    // Recalculate each symbol
    for (const symbol of symbols) {
      await this.recalculateHolding(userId, symbol);
    }
  }
}

// Singleton instance
export const holdingRecalculationService = new HoldingRecalculationService();
