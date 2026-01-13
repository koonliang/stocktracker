import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { yahooFinanceService } from '@/lib/external/yahoo-finance.service';

// GET /api/transactions/validate-ticker?symbol=XXX - Validate ticker symbol
export async function GET(request: NextRequest) {
  try {
    // Authenticate user
    await authenticateRequest(request);

    // Get symbol from query params
    const { searchParams } = new URL(request.url);
    const symbol = searchParams.get('symbol');

    // Validate symbol parameter
    if (!symbol || symbol.trim() === '') {
      return NextResponse.json(
        {
          success: true,
          data: {
            valid: false,
            symbol: symbol || '',
            errorMessage: 'Symbol is required',
          },
        },
        { status: 200 },
      );
    }

    const upperSymbol = symbol.toUpperCase().trim();

    // Try to fetch quote from Yahoo Finance
    try {
      const quote = await yahooFinanceService.getQuote(upperSymbol);

      // Check if quote has valid price data
      if (!quote.regularMarketPrice || quote.regularMarketPrice === 0) {
        return NextResponse.json(
          {
            success: true,
            data: {
              valid: false,
              symbol: upperSymbol,
              errorMessage: 'Invalid ticker symbol',
            },
          },
          { status: 200 },
        );
      }

      // Valid ticker
      return NextResponse.json(
        {
          success: true,
          data: {
            valid: true,
            symbol: upperSymbol,
            companyName: quote.shortName || upperSymbol,
          },
        },
        { status: 200 },
      );
    } catch (error) {
      // Yahoo Finance error - invalid ticker
      console.error('Ticker validation error:', error);
      return NextResponse.json(
        {
          success: true,
          data: {
            valid: false,
            symbol: upperSymbol,
            errorMessage: 'Invalid ticker symbol',
          },
        },
        { status: 200 },
      );
    }
  } catch (error) {
    console.error('Validate ticker error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to validate ticker' },
      { status: 500 },
    );
  }
}
