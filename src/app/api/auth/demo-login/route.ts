import { NextResponse } from 'next/server';
import { authService } from '@/lib/auth/auth.service';

export async function POST() {
  try {
    // Create demo account and get auth response
    const authResponse = await authService.demoLogin();

    return NextResponse.json(
      {
        success: true,
        data: authResponse,
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Demo login error:', error);
    return NextResponse.json(
      { success: false, message: 'Failed to create demo account' },
      { status: 500 },
    );
  }
}
