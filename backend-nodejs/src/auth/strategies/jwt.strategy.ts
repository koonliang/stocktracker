import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { UserService } from '../../user/user.service';

export interface JwtPayload {
  sub: string; // email (username in Spring Security)
  iat: number; // issued at
  exp: number; // expiration
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    private configService: ConfigService,
    private userService: UserService,
  ) {
    const jwtSecret = configService.get<string>('JWT_SECRET');
    if (!jwtSecret) {
      throw new Error('JWT_SECRET is not defined in environment variables');
    }

    // Decode BASE64 secret to match Java's implementation
    // Java: byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    const secretBuffer = Buffer.from(jwtSecret, 'base64');

    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: secretBuffer,
      algorithms: ['HS256'], // HMAC-SHA256 to match Java
    });
  }

  async validate(payload: JwtPayload) {
    // payload.sub contains the email (username in Spring Security)
    const user = await this.userService.findByEmail(payload.sub);

    if (!user) {
      throw new UnauthorizedException('User not found');
    }

    if (!user.enabled) {
      throw new UnauthorizedException('User account is disabled');
    }

    // Return user info to be attached to request.user
    // This matches what @CurrentUser decorator expects
    return {
      sub: user.email,
      userId: Number(user.id), // Convert BigInt to number for JSON serialization
      iat: payload.iat,
      exp: payload.exp,
    };
  }
}
