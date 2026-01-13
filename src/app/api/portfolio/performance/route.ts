import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { portfolioService } from '@/lib/portfolio/portfolio.service';

export async function GET(request: NextRequest) {
  try {
    // Authenticate user
    const user = await authenticateRequest(request);

    // Get time range from query params
    const { searchParams } = new URL(request.url);
    const range = (searchParams.get('range') || '1y') as
      | '7d'
      | '1mo'
      | '3mo'
      | 'ytd'
      | '1y'
      | 'all';

    // Get performance history
    const performance = await portfolioService.getPerformanceHistory(user.userId, range);

    return NextResponse.json(
      {
        success: true,
        data: performance,
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Performance history error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to fetch performance history' },
      { status: 500 },
    );
  }
}
