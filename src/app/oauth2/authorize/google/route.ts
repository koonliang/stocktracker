import { NextRequest, NextResponse } from 'next/server';

/**
 * GET /oauth2/authorize/google
 * Redirects user to Google OAuth2 authorization page
 */
export async function GET(request: NextRequest) {
  const googleClientId = process.env.GOOGLE_CLIENT_ID;

  if (!googleClientId) {
    return NextResponse.json(
      { error: 'Google OAuth is not configured' },
      { status: 500 }
    );
  }

  // Build the callback URL (backend API endpoint that handles the OAuth callback)
  const callbackUrl = `${request.nextUrl.origin}/api/auth/oauth2/callback/google`;

  // Build Google OAuth2 authorization URL
  const params = new URLSearchParams({
    client_id: googleClientId,
    redirect_uri: callbackUrl,
    response_type: 'code',
    scope: 'email profile openid',
    access_type: 'offline',
    prompt: 'consent',
  });

  const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;

  // Redirect user to Google's OAuth consent screen
  return NextResponse.redirect(authUrl);
}
