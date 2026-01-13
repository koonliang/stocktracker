import { NextRequest, NextResponse } from 'next/server';
import { userService } from '@/lib/user/user.service';
import { jwtService } from '@/lib/auth/jwt.service';
import { AuthProvider, Role } from '@/lib/database/types';

interface GoogleTokenResponse {
  access_token: string;
  expires_in: number;
  scope: string;
  token_type: string;
  id_token: string;
}

interface GoogleUserInfo {
  sub: string; // Google user ID
  email: string;
  email_verified: boolean;
  name: string;
  given_name?: string;
  family_name?: string;
  picture?: string;
  locale?: string;
}

/**
 * GET /api/auth/oauth2/callback/google
 * Handles OAuth2 callback from Google
 */
export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const code = searchParams.get('code');
  const error = searchParams.get('error');

  // Handle OAuth error
  if (error) {
    const errorDescription = searchParams.get('error_description') || 'OAuth authentication failed';
    return NextResponse.redirect(
      `${request.nextUrl.origin}/oauth2/redirect?error=${encodeURIComponent(errorDescription)}`
    );
  }

  // Validate authorization code
  if (!code) {
    return NextResponse.redirect(
      `${request.nextUrl.origin}/oauth2/redirect?error=${encodeURIComponent('No authorization code received')}`
    );
  }

  try {
    const googleClientId = process.env.GOOGLE_CLIENT_ID;
    const googleClientSecret = process.env.GOOGLE_CLIENT_SECRET;
    const callbackUrl = `${request.nextUrl.origin}/api/auth/oauth2/callback/google`;

    if (!googleClientId || !googleClientSecret) {
      throw new Error('Google OAuth credentials not configured');
    }

    // Exchange authorization code for access token
    const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        code,
        client_id: googleClientId,
        client_secret: googleClientSecret,
        redirect_uri: callbackUrl,
        grant_type: 'authorization_code',
      }),
    });

    if (!tokenResponse.ok) {
      const errorData = await tokenResponse.text();
      console.error('Token exchange failed:', errorData);
      throw new Error('Failed to exchange authorization code for access token');
    }

    const tokenData: GoogleTokenResponse = await tokenResponse.json();

    // Get user info from Google
    const userInfoResponse = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
      headers: {
        Authorization: `Bearer ${tokenData.access_token}`,
      },
    });

    if (!userInfoResponse.ok) {
      throw new Error('Failed to fetch user info from Google');
    }

    const googleUser: GoogleUserInfo = await userInfoResponse.json();

    // Validate email
    if (!googleUser.email) {
      throw new Error('No email found in Google profile');
    }

    // Check if user exists
    let user = await userService.findByEmail(googleUser.email);

    if (user) {
      // User exists - verify they're using Google or local auth
      if (user.auth_provider !== AuthProvider.GOOGLE && user.auth_provider !== AuthProvider.LOCAL) {
        throw new Error(`Email already registered with ${user.auth_provider} provider`);
      }

      // Update profile if using Google
      if (user.auth_provider === AuthProvider.GOOGLE) {
        user = await userService.update(Number(user.id), {
          name: googleUser.name,
          profile_image_url: googleUser.picture,
          oauth_provider_id: googleUser.sub,
          updated_at: new Date(),
        });
      }
    } else {
      // Create new user with Google auth
      user = await userService.create({
        email: googleUser.email,
        name: googleUser.name,
        auth_provider: AuthProvider.GOOGLE,
        oauth_provider_id: googleUser.sub,
        profile_image_url: googleUser.picture,
        role: Role.USER,
        enabled: true,
        is_demo_account: false,
        password: null, // No password for OAuth users
        created_at: new Date(),
        updated_at: new Date(),
      });
    }

    // Generate JWT token
    const token = jwtService.generateToken(user);

    // Redirect to frontend OAuth redirect page with user data
    const redirectUrl = new URL('/oauth2/redirect', request.nextUrl.origin);
    redirectUrl.searchParams.set('token', token);
    redirectUrl.searchParams.set('userId', user.id.toString());
    redirectUrl.searchParams.set('email', user.email);
    redirectUrl.searchParams.set('name', user.name);

    return NextResponse.redirect(redirectUrl.toString());
  } catch (error) {
    console.error('OAuth callback error:', error);
    const errorMessage = error instanceof Error ? error.message : 'Authentication failed';
    return NextResponse.redirect(
      `${request.nextUrl.origin}/oauth2/redirect?error=${encodeURIComponent(errorMessage)}`
    );
  }
}
