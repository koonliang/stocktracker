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
}

// Singleton instance
export const transactionService = new TransactionService();
