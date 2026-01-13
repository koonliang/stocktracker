import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { portfolioService } from '@/lib/portfolio/portfolio.service';

export async function GET(request: NextRequest) {
  try {
    // Authenticate user
    const user = await authenticateRequest(request);

    // Get portfolio data
    const portfolio = await portfolioService.getPortfolio(user.userId, false);

    return NextResponse.json(
      {
        success: true,
        data: portfolio,
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Portfolio fetch error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to fetch portfolio' },
      { status: 500 },
    );
  }
}
