import { Controller, Get, UseGuards } from '@nestjs/common';
import { GoogleAuthGuard } from './guards/google-auth.guard';

/**
 * OAuth2 authorization controller
 * Handles /oauth2/authorize/* routes (not under /api)
 */
@Controller('oauth2/authorize')
export class OAuth2Controller {
  /**
   * GET /oauth2/authorize/google
   * Redirect to Google OAuth2 authorization
   */
  @Get('google')
  @UseGuards(GoogleAuthGuard)
  async googleAuth() {
    // Guard redirects to Google
  }
}
