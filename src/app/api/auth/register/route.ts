import { NextRequest, NextResponse } from 'next/server';
import { authService } from '@/lib/auth/auth.service';
import { SignupDto } from '@/lib/auth/dto';
import { BadRequestException } from '@/lib/common/exceptions';

export async function POST(request: NextRequest) {
  try {
    const body: SignupDto = await request.json();

    // Validate required fields
    if (!body.name || !body.email || !body.password || !body.confirmPassword) {
      return NextResponse.json(
        {
          success: false,
          message: 'All fields are required',
        },
        { status: 400 },
      );
    }

    // Perform registration
    const authResponse = await authService.register(body);

    return NextResponse.json(
      {
        success: true,
        data: authResponse,
      },
      { status: 201 },
    );
  } catch (error) {
    if (error instanceof BadRequestException) {
      return NextResponse.json(
        { success: false, message: error.message },
        { status: 400 },
      );
    }

    console.error('Registration error:', error);
    return NextResponse.json(
      { success: false, message: 'Internal server error' },
      { status: 500 },
    );
  }
}
