import { prisma } from '../database/prisma';
import { Holding } from '../database/types';

export class HoldingService {
  /**
   * Get all holdings for a user
   */
  async findByUserId(userId: number): Promise<Holding[]> {
    return prisma.holdings.findMany({
      where: { user_id: BigInt(userId) },
      orderBy: { symbol: 'asc' },
    });
  }

  /**
   * Get a specific holding by user and symbol
   */
  async findByUserIdAndSymbol(userId: number, symbol: string): Promise<Holding | null> {
    return prisma.holdings.findUnique({
      where: {
        user_id_symbol: {
          user_id: BigInt(userId),
          symbol,
        },
      },
    });
  }

  /**
   * Create or update a holding
   */
  async upsert(
    userId: number,
    symbol: string,
    data: {
      companyName: string;
      shares: number;
      averageCost: number;
    },
  ): Promise<Holding> {
    return prisma.holdings.upsert({
      where: {
        user_id_symbol: {
          user_id: BigInt(userId),
          symbol,
        },
      },
      create: {
        user_id: BigInt(userId),
        symbol,
        company_name: data.companyName,
        shares: data.shares,
        average_cost: data.averageCost,
      },
      update: {
        company_name: data.companyName,
        shares: data.shares,
        average_cost: data.averageCost,
      },
    });
  }

  /**
   * Delete a holding
   */
  async delete(userId: number, symbol: string): Promise<void> {
    await prisma.holdings.delete({
      where: {
        user_id_symbol: {
          user_id: BigInt(userId),
          symbol,
        },
      },
    });
  }

  /**
   * Delete all holdings for a user
   */
  async deleteByUserId(userId: number): Promise<void> {
    await prisma.holdings.deleteMany({
      where: { user_id: BigInt(userId) },
    });
  }
}

// Singleton instance
export const holdingService = new HoldingService();
