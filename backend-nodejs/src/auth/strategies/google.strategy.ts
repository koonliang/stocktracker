import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy, VerifyCallback } from 'passport-google-oauth20';
import { UserService } from '../../user/user.service';
import { AuthProvider, Role } from '../../database/types';

interface GoogleProfile {
  id: string;
  name?: {
    givenName?: string;
    familyName?: string;
  };
  emails?: Array<{ value: string }>;
  photos?: Array<{ value: string }>;
}

@Injectable()
export class GoogleStrategy extends PassportStrategy(Strategy, 'google') {
  constructor(
    private configService: ConfigService,
    private userService: UserService,
  ) {
    const port = configService.get<number>('PORT', 8080);
    // Callback URL should point to backend, not frontend
    const callbackURL = `http://localhost:${port}/api/auth/oauth2/callback/google`;

    super({
      clientID: configService.get<string>('GOOGLE_CLIENT_ID') || '',
      clientSecret: configService.get<string>('GOOGLE_CLIENT_SECRET') || '',
      callbackURL,
      scope: ['email', 'profile', 'openid'],
    });
  }

  async validate(
    accessToken: string,
    refreshToken: string,
    profile: GoogleProfile,
    done: VerifyCallback,
  ): Promise<void> {
    const { id, name, emails, photos } = profile;

    if (!emails || emails.length === 0) {
      done(new Error('No email found in Google profile'), false);
      return;
    }

    const email = emails[0].value;
    const fullName = name
      ? `${name.givenName ?? ''} ${name.familyName ?? ''}`.trim()
      : email;
    const profileImage = photos && photos.length > 0 ? photos[0].value : null;

    try {
      // Check if user exists
      let user = await this.userService.findByEmail(email);

      if (user) {
        // User exists - verify they're using Google auth
        if (
          user.auth_provider !== AuthProvider.GOOGLE &&
          user.auth_provider !== AuthProvider.LOCAL
        ) {
          done(
            new Error(
              `Email already registered with ${user.auth_provider} provider`,
            ),
            false,
          );
          return;
        }

        // Update profile if using Google
        if (user.auth_provider === AuthProvider.GOOGLE) {
          user = await this.userService.update(Number(user.id), {
            name: fullName,
            profile_image_url: profileImage ?? undefined,
            oauth_provider_id: id,
            updated_at: new Date(),
          });
        }
      } else {
        // Create new user with Google auth
        user = await this.userService.create({
          email,
          name: fullName,
          auth_provider: AuthProvider.GOOGLE,
          oauth_provider_id: id,
          profile_image_url: profileImage ?? undefined,
          role: Role.USER,
          enabled: true,
          is_demo_account: false,
          password: null, // No password for OAuth users
          created_at: new Date(),
          updated_at: new Date(),
        });
      }

      done(null, user);
    } catch (error) {
      done(error as Error, false);
    }
  }
}
