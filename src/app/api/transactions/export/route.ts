import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { transactionService } from '@/lib/transaction/transaction.service';

/**
 * Escape CSV values to prevent injection and format issues
 */
function escapeCSV(value: string | null): string {
  if (value === null || value === undefined) return '';
  // Replace double quotes with two double quotes
  return value.replace(/"/g, '""');
}

// GET /api/transactions/export - Export transactions as CSV
export async function GET(request: NextRequest) {
  try {
    // Authenticate user
    const user = await authenticateRequest(request);

    // Fetch transactions
    const transactions = await transactionService.findByUserId(user.userId);

    // Build CSV
    const lines: string[] = [];

    // Header row
    lines.push('Type,Symbol,Company Name,Date,Shares,Price Per Share,Broker Fee,Total Amount,Notes');

    // Data rows
    for (const tx of transactions) {
      const brokerFee = tx.broker_fee !== null ? Number(tx.broker_fee).toString() : '';
      const notes = escapeCSV(tx.notes);
      const companyName = escapeCSV(tx.company_name);

      lines.push(
        `${tx.type},${tx.symbol},"${companyName}",${tx.transaction_date.toISOString().split('T')[0]},${Number(tx.shares)},${Number(tx.price_per_share)},${brokerFee},${Number(tx.total_amount)},"${notes}"`,
      );
    }

    const csv = lines.join('\n');

    // Return as downloadable CSV file
    return new NextResponse(csv, {
      status: 200,
      headers: {
        'Content-Type': 'text/csv',
        'Content-Disposition': `attachment; filename="transactions-${new Date().toISOString().split('T')[0]}.csv"`,
      },
    });
  } catch (error) {
    console.error('Export transactions error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to export transactions' },
      { status: 500 },
    );
  }
}
