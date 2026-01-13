import { prisma } from '../database/prisma';
import { Transaction, TransactionType } from '../database/types';
import { Prisma } from '@prisma/client';
import Decimal from 'decimal.js';

// Configure Decimal for financial calculations
Decimal.set({ rounding: Decimal.ROUND_HALF_UP, precision: 20 });

export interface TransactionCreateData {
  userId: number;
  type: TransactionType;
  symbol: string;
  companyName: string;
  shares: number;
  pricePerShare: number;
  transactionDate: Date;
  brokerFee?: number;
  notes?: string;
}

export class TransactionService {
  /**
   * Get all transactions for a user
   */
  async findByUserId(userId: number): Promise<Transaction[]> {
    return prisma.transactions.findMany({
      where: { user_id: BigInt(userId) },
      orderBy: { transaction_date: 'desc' },
    });
  }

  /**
   * Get transactions for a user and symbol
   */
  async findByUserIdAndSymbol(userId: number, symbol: string): Promise<Transaction[]> {
    return prisma.transactions.findMany({
      where: {
        user_id: BigInt(userId),
        symbol,
      },
      orderBy: { transaction_date: 'asc' },
    });
  }

  /**
   * Create a new transaction
   */
  async create(data: TransactionCreateData): Promise<Transaction> {
    const shares = new Decimal(data.shares);
    const pricePerShare = new Decimal(data.pricePerShare);
    const brokerFee = data.brokerFee ? new Decimal(data.brokerFee) : new Decimal(0);

    // Calculate total amount
    const totalAmount = shares.mul(pricePerShare).plus(brokerFee);

    return prisma.transactions.create({
      data: {
        user_id: BigInt(data.userId),
        type: data.type,
        symbol: data.symbol,
        company_name: data.companyName,
        shares: shares.toNumber(),
        price_per_share: pricePerShare.toNumber(),
        transaction_date: data.transactionDate,
        broker_fee: data.brokerFee ?? null,
        total_amount: totalAmount.toNumber(),
        notes: data.notes ?? null,
      },
    });
  }

  /**
   * Create multiple transactions (bulk insert)
   */
  async createMany(transactions: TransactionCreateData[]): Promise<number> {
    const data = transactions.map((tx) => {
      const shares = new Decimal(tx.shares);
      const pricePerShare = new Decimal(tx.pricePerShare);
      const brokerFee = tx.brokerFee ? new Decimal(tx.brokerFee) : new Decimal(0);
      const totalAmount = shares.mul(pricePerShare).plus(brokerFee);

      return {
        user_id: BigInt(tx.userId),
        type: tx.type,
        symbol: tx.symbol,
        company_name: tx.companyName,
        shares: shares.toNumber(),
        price_per_share: pricePerShare.toNumber(),
        transaction_date: tx.transactionDate,
        broker_fee: tx.brokerFee ?? null,
        total_amount: totalAmount.toNumber(),
        notes: tx.notes ?? null,
      };
    });

    const result = await prisma.transactions.createMany({ data });
    return result.count;
  }

  /**
   * Update a transaction
   */
  async update(id: number, data: Partial<TransactionCreateData>): Promise<Transaction> {
    const updateData: Prisma.transactionsUpdateInput = {};

    if (data.type) updateData.type = data.type;
    if (data.symbol) updateData.symbol = data.symbol;
    if (data.companyName) updateData.company_name = data.companyName;
    if (data.transactionDate) updateData.transaction_date = data.transactionDate;
    if (data.notes !== undefined) updateData.notes = data.notes;

    // Recalculate total amount if shares or price changed
    if (data.shares !== undefined || data.pricePerShare !== undefined || data.brokerFee !== undefined) {
      const existing = await prisma.transactions.findUnique({
        where: { id: BigInt(id) },
      });

      if (!existing) {
        throw new Error('Transaction not found');
      }

      const shares = new Decimal(data.shares ?? existing.shares.toString());
      const pricePerShare = new Decimal(data.pricePerShare ?? existing.price_per_share.toString());
      const brokerFee = new Decimal(
        data.brokerFee !== undefined
          ? data.brokerFee
          : existing.broker_fee
            ? existing.broker_fee.toString()
            : 0,
      );

      updateData.shares = shares.toNumber();
      updateData.price_per_share = pricePerShare.toNumber();
      updateData.broker_fee = data.brokerFee !== undefined ? data.brokerFee : existing.broker_fee;
      updateData.total_amount = shares.mul(pricePerShare).plus(brokerFee).toNumber();
    }

    return prisma.transactions.update({
      where: { id: BigInt(id) },
      data: updateData,
    });
  }

  /**
   * Delete a transaction
   */
  async delete(id: number): Promise<void> {
    await prisma.transactions.delete({
      where: { id: BigInt(id) },
    });
  }

  /**
   * Delete all transactions for a user
   */
  async deleteByUserId(userId: number): Promise<void> {
    await prisma.transactions.deleteMany({
      where: { user_id: BigInt(userId) },
    });
  }

  /**
   * Delete all transactions for a user and symbol
   */
  async deleteByUserIdAndSymbol(userId: number, symbol: string): Promise<void> {
    await prisma.transactions.deleteMany({
      where: {
        user_id: BigInt(userId),
        symbol,
      },
    });
  }

  /**
   * Check if user has any BUY transactions for a symbol
   */
  async existsByUserIdAndSymbolAndType(
    userId: number,
    symbol: string,
    type: TransactionType,
  ): Promise<boolean> {
    const count = await prisma.transactions.count({
      where: {
        user_id: BigInt(userId),
        symbol,
        type,
      },
    });
    return count > 0;
  }

  /**
   * Find the earliest BUY date for a symbol
   */
  async findEarliestBuyDate(userId: number, symbol: string): Promise<Date | null> {
    const transaction = await prisma.transactions.findFirst({
      where: {
        user_id: BigInt(userId),
        symbol,
        type: TransactionType.BUY,
      },
      orderBy: { transaction_date: 'asc' },
      select: { transaction_date: true },
    });
    return transaction?.transaction_date ?? null;
  }

  /**
   * Calculate net shares owned (total BUY shares - total SELL shares) up to a specific date
   * If sellDate is provided, only count transactions before that date
   */
  async calculateNetShares(
    userId: number,
    symbol: string,
    beforeDate?: Date,
  ): Promise<number> {
    const where: Prisma.transactionsWhereInput = {
      user_id: BigInt(userId),
      symbol,
    };

    if (beforeDate) {
      where.transaction_date = {
        lt: beforeDate,
      };
    }

    const transactions = await prisma.transactions.findMany({
      where,
      select: {
        type: true,
        shares: true,
      },
    });

    let netShares = new Decimal(0);
    for (const tx of transactions) {
      const shares = new Decimal(tx.shares.toString());
      if (tx.type === TransactionType.BUY) {
        netShares = netShares.plus(shares);
      } else if (tx.type === TransactionType.SELL) {
        netShares = netShares.minus(shares);
      }
    }

    return netShares.toNumber();
  }

  /**
   * Validate a sell transaction
   */
  async validateSellTransaction(
    userId: number,
    symbol: string,
    sharesToSell: number,
    sellDate: Date,
    excludeTransactionId?: number,
  ): Promise<{ valid: boolean; error?: string }> {
    // Check if user has any buy transactions for this symbol
    const hasBuyTransactions = await this.existsByUserIdAndSymbolAndType(
      userId,
      symbol,
      TransactionType.BUY,
    );

    if (!hasBuyTransactions) {
      return {
        valid: false,
        error: `Cannot sell ${symbol}: no buy transactions exist`,
      };
    }

    // Check sell date is not before first buy
    const earliestBuy = await this.findEarliestBuyDate(userId, symbol);
    if (earliestBuy && sellDate < earliestBuy) {
      const earliestBuyStr = earliestBuy.toISOString().split('T')[0];
      return {
        valid: false,
        error: `Sell date cannot be before first buy date (${earliestBuyStr})`,
      };
    }

    // Calculate net shares up to the sell date
    // If updating, we need to exclude the current transaction from the calculation
    let netShares = await this.calculateNetShares(userId, symbol, sellDate);

    // If we're updating a transaction, add back the shares from that transaction
    if (excludeTransactionId) {
      const existingTx = await prisma.transactions.findUnique({
        where: { id: BigInt(excludeTransactionId) },
      });

      if (existingTx && existingTx.transaction_date < sellDate) {
        const existingShares = new Decimal(existingTx.shares.toString());
        if (existingTx.type === TransactionType.SELL) {
          // Add back the shares we're replacing
          netShares = new Decimal(netShares).plus(existingShares).toNumber();
        }
      }
    }

    // Check if user has enough shares to sell
    const sharesToSellDecimal = new Decimal(sharesToSell);
    const netSharesDecimal = new Decimal(netShares);

    if (netSharesDecimal.lessThan(sharesToSellDecimal)) {
      return {
        valid: false,
        error: `Cannot sell ${sharesToSell} shares of ${symbol}: only ${netShares} shares owned`,
      };
    }

    return { valid: true };
  }
}

// Singleton instance
export const transactionService = new TransactionService();
