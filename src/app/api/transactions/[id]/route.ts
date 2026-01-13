import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { transactionService } from '@/lib/transaction/transaction.service';
import { holdingRecalculationService } from '@/lib/holding/holding-recalculation.service';
import { prisma } from '@/lib/database/prisma';
import { TransactionType } from '@/lib/database/types';
import { cacheService } from '@/lib/cache/cache.service';

// PUT /api/transactions/:id - Update a transaction
export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    // Authenticate user
    const user = await authenticateRequest(request);

    const { id } = await params;
    const transactionId = parseInt(id, 10);
    if (isNaN(transactionId)) {
      return NextResponse.json({ success: false, message: 'Invalid transaction ID' }, { status: 400 });
    }

    // Check if transaction exists and belongs to user
    const existing = await prisma.transactions.findUnique({
      where: { id: BigInt(transactionId) },
    });

    if (!existing) {
      return NextResponse.json({ success: false, message: 'Transaction not found' }, { status: 404 });
    }

    if (Number(existing.user_id) !== user.userId) {
      return NextResponse.json({ success: false, message: 'Forbidden' }, { status: 403 });
    }

    // Parse request body
    const body = await request.json();

    // Validate transaction type if provided
    if (body.type && body.type !== TransactionType.BUY && body.type !== TransactionType.SELL) {
      return NextResponse.json(
        { success: false, message: 'Invalid transaction type' },
        { status: 400 },
      );
    }

    // Validate sell transaction if type is SELL (either existing or being updated to SELL)
    const newType = body.type || existing.type;
    if (newType === TransactionType.SELL) {
      const newSymbol = body.symbol ? body.symbol.toUpperCase() : existing.symbol;
      const newShares = body.shares !== undefined ? Number(body.shares) : Number(existing.shares);
      const newDate = body.transactionDate ? new Date(body.transactionDate) : existing.transaction_date;

      const validation = await transactionService.validateSellTransaction(
        user.userId,
        newSymbol,
        newShares,
        newDate,
        transactionId, // Exclude current transaction from validation
      );

      if (!validation.valid) {
        return NextResponse.json(
          { success: false, message: validation.error },
          { status: 400 },
        );
      }
    }

    const oldSymbol = existing.symbol;

    // Update transaction
    const transaction = await transactionService.update(transactionId, {
      type: body.type,
      symbol: body.symbol ? body.symbol.toUpperCase() : undefined,
      companyName: body.companyName,
      shares: body.shares !== undefined ? Number(body.shares) : undefined,
      pricePerShare: body.pricePerShare !== undefined ? Number(body.pricePerShare) : undefined,
      transactionDate: body.transactionDate ? new Date(body.transactionDate) : undefined,
      brokerFee: body.brokerFee !== undefined ? Number(body.brokerFee) : undefined,
      notes: body.notes !== undefined ? body.notes : undefined,
    });

    // Recalculate holdings for affected symbols
    await holdingRecalculationService.recalculateHolding(user.userId, transaction.symbol);
    if (oldSymbol !== transaction.symbol) {
      await holdingRecalculationService.recalculateHolding(user.userId, oldSymbol);
    }

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
      { status: 200 },
    );
  } catch (error) {
    console.error('Update transaction error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to update transaction' },
      { status: 500 },
    );
  }
}

// DELETE /api/transactions/:id - Delete a transaction
export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    // Authenticate user
    const user = await authenticateRequest(request);

    const { id } = await params;
    const transactionId = parseInt(id, 10);
    if (isNaN(transactionId)) {
      return NextResponse.json({ success: false, message: 'Invalid transaction ID' }, { status: 400 });
    }

    // Check if transaction exists and belongs to user
    const existing = await prisma.transactions.findUnique({
      where: { id: BigInt(transactionId) },
    });

    if (!existing) {
      return NextResponse.json({ success: false, message: 'Transaction not found' }, { status: 404 });
    }

    if (Number(existing.user_id) !== user.userId) {
      return NextResponse.json({ success: false, message: 'Forbidden' }, { status: 403 });
    }

    const symbol = existing.symbol;

    // Delete transaction
    await transactionService.delete(transactionId);

    // Recalculate holdings for this symbol
    await holdingRecalculationService.recalculateHolding(user.userId, symbol);

    // Evict cache
    await cacheService.evictUserCache(user.userId);

    return NextResponse.json(
      {
        success: true,
        message: 'Transaction deleted successfully',
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Delete transaction error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to delete transaction' },
      { status: 500 },
    );
  }
}
