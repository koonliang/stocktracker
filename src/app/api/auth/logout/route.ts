import { NextResponse } from 'next/server';
import { authService } from '@/lib/auth/auth.service';

export async function POST() {
  try {
    const result = authService.logout();

    return NextResponse.json(
      {
        success: true,
        message: result.message,
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Logout error:', error);
    return NextResponse.json(
      { success: false, message: 'Internal server error' },
      { status: 500 },
    );
  }
}
