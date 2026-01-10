import {
  Controller,
  Post,
  Body,
  Get,
  UseGuards,
  Req,
  Res,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { Request, Response } from 'express';
import { AuthService } from './auth.service';
import { JwtService } from './jwt.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { GoogleAuthGuard } from './guards/google-auth.guard';
import { LoginDto, SignupDto, AuthResponse } from './dto';
import { CurrentUser } from '../common/decorators';
import type { JwtPayload } from '../common/decorators';
import { User } from '../database/types';

@Controller('api/auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private jwtService: JwtService,
    private configService: ConfigService,
  ) {}

  /**
   * POST /api/auth/login
   * Email/password authentication
   */
  @Post('login')
  async login(@Body() loginDto: LoginDto): Promise<AuthResponse> {
    return this.authService.login(loginDto);
  }

  /**
   * POST /api/auth/register
   * User registration with password validation
   */
  @Post('register')
  async register(@Body() signupDto: SignupDto): Promise<AuthResponse> {
    return this.authService.register(signupDto);
  }

  /**
   * POST /api/auth/logout
   * Logout (stateless - client discards token)
   */
  @Post('logout')
  logout(): { message: string } {
    return this.authService.logout();
  }

  /**
   * GET /api/auth/oauth2/callback/google
   * Google OAuth2 callback
   * Matches Java: /api/auth/oauth2/callback/{registrationId}
   */
  @Get('oauth2/callback/google')
  @UseGuards(GoogleAuthGuard)
  googleAuthCallback(@Req() req: Request, @Res() res: Response) {
    const user = req.user as User;

    // Generate JWT token
    const token = this.jwtService.generateToken(user);

    // Build redirect URL with query parameters (matches Java implementation)
    const redirectUri = this.configService.get<string>(
      'OAUTH2_REDIRECT_URI',
      'http://localhost:3000/oauth2/redirect',
    );

    const targetUrl = `${redirectUri}?token=${token}&userId=${user.id}&email=${encodeURIComponent(user.email)}&name=${encodeURIComponent(user.name)}`;

    // Redirect to frontend
    res.redirect(targetUrl);
  }

  /**
   * GET /api/auth/test (protected route for testing)
   * Test JWT authentication
   */
  @Get('test')
  @UseGuards(JwtAuthGuard)
  testProtectedRoute(@CurrentUser() user: JwtPayload) {
    return {
      message: 'JWT authentication is working!',
      user: {
        email: user.sub,
        userId: user.userId,
      },
    };
  }
}
