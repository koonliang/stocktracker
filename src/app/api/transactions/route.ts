import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { transactionService } from '@/lib/transaction/transaction.service';
import { holdingRecalculationService } from '@/lib/holding/holding-recalculation.service';
import { TransactionType } from '@/lib/database/types';
import { cacheService } from '@/lib/cache/cache.service';

// GET /api/transactions - Get all transactions for authenticated user
export async function GET(request: NextRequest) {
  try {
    // Authenticate user
    const user = await authenticateRequest(request);

    // Fetch transactions
    const transactions = await transactionService.findByUserId(user.userId);

    // Transform to response format
    const response = transactions.map((tx) => ({
      id: Number(tx.id),
      type: tx.type,
      symbol: tx.symbol,
      companyName: tx.company_name,
      transactionDate: tx.transaction_date.toISOString().split('T')[0], // YYYY-MM-DD
      shares: Number(tx.shares),
      pricePerShare: Number(tx.price_per_share),
      brokerFee: tx.broker_fee ? Number(tx.broker_fee) : null,
      totalAmount: Number(tx.total_amount),
      notes: tx.notes,
      createdAt: tx.created_at.toISOString(),
      updatedAt: tx.updated_at ? tx.updated_at.toISOString() : tx.created_at.toISOString(),
    }));

    return NextResponse.json(
      {
        success: true,
        data: response,
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Get transactions error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to fetch transactions' },
      { status: 500 },
    );
  }
}

// POST /api/transactions - Create a new transaction
export async function POST(request: NextRequest) {
  try {
    // Authenticate user
    const user = await authenticateRequest(request);

    // Parse request body
    const body = await request.json();

    // Validate required fields
    if (
      !body.type ||
      !body.symbol ||
      !body.transactionDate ||
      body.shares === undefined ||
      body.pricePerShare === undefined
    ) {
      return NextResponse.json(
        { success: false, message: 'Missing required fields' },
        { status: 400 },
      );
    }

    // Validate transaction type
    if (body.type !== TransactionType.BUY && body.type !== TransactionType.SELL) {
      return NextResponse.json(
        { success: false, message: 'Invalid transaction type' },
        { status: 400 },
      );
    }

    // Validate sell transaction
    if (body.type === TransactionType.SELL) {
      const validation = await transactionService.validateSellTransaction(
        user.userId,
        body.symbol.toUpperCase(),
        Number(body.shares),
        new Date(body.transactionDate),
      );

      if (!validation.valid) {
        return NextResponse.json(
          { success: false, message: validation.error },
          { status: 400 },
        );
      }
    }

    // Create transaction
    const transaction = await transactionService.create({
      userId: user.userId,
      type: body.type,
      symbol: body.symbol.toUpperCase(),
      companyName: body.companyName || body.symbol.toUpperCase(),
      shares: Number(body.shares),
      pricePerShare: Number(body.pricePerShare),
      transactionDate: new Date(body.transactionDate),
      brokerFee: body.brokerFee ? Number(body.brokerFee) : undefined,
      notes: body.notes || undefined,
    });

    // Recalculate holdings for this symbol
    await holdingRecalculationService.recalculateHolding(user.userId, transaction.symbol);

    // Evict cache
    await cacheService.evictUserCache(user.userId);

    // Return response
    const response = {
      id: Number(transaction.id),
      type: transaction.type,
      symbol: transaction.symbol,
      companyName: transaction.company_name,
      transactionDate: transaction.transaction_date.toISOString().split('T')[0],
      shares: Number(transaction.shares),
      pricePerShare: Number(transaction.price_per_share),
      brokerFee: transaction.broker_fee ? Number(transaction.broker_fee) : null,
      totalAmount: Number(transaction.total_amount),
      notes: transaction.notes,
      createdAt: transaction.created_at.toISOString(),
      updatedAt: transaction.updated_at
        ? transaction.updated_at.toISOString()
        : transaction.created_at.toISOString(),
    };

    return NextResponse.json(
      {
        success: true,
        data: response,
      },
      { status: 201 },
    );
  } catch (error) {
    console.error('Create transaction error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to create transaction' },
      { status: 500 },
    );
  }
}
