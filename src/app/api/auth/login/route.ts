import { NextRequest, NextResponse } from 'next/server';
import { authService } from '@/lib/auth/auth.service';
import { LoginDto } from '@/lib/auth/dto';
import {
  BadRequestException,
  UnauthorizedException,
} from '@/lib/common/exceptions';

export async function POST(request: NextRequest) {
  try {
    const body: LoginDto = await request.json();

    // Validate required fields
    if (!body.email || !body.password) {
      return NextResponse.json(
        { success: false, message: 'Email and password are required' },
        { status: 400 },
      );
    }

    // Perform login
    const authResponse = await authService.login(body);

    return NextResponse.json(
      {
        success: true,
        data: authResponse,
      },
      { status: 200 },
    );
  } catch (error) {
    if (error instanceof UnauthorizedException) {
      return NextResponse.json(
        { success: false, message: error.message },
        { status: 401 },
      );
    }

    if (error instanceof BadRequestException) {
      return NextResponse.json(
        { success: false, message: error.message },
        { status: 400 },
      );
    }

    console.error('Login error:', error);
    return NextResponse.json(
      { success: false, message: 'Internal server error' },
      { status: 500 },
    );
  }
}
