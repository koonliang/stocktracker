import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService as NestJwtService } from '@nestjs/jwt';
import { User } from '../database/types';

@Injectable()
export class JwtService {
  constructor(
    private nestJwtService: NestJwtService,
    private configService: ConfigService,
  ) {}

  /**
   * Generate JWT token matching Java's JwtTokenProvider.generateToken()
   * Token structure:
   * - sub: email (username in Spring Security)
   * - iat: issued at timestamp
   * - exp: expiration timestamp
   */
  generateToken(user: User): string {
    const payload = {
      sub: user.email, // username in Spring Security UserDetails
    };

    // The JwtModule config will automatically add iat and exp
    return this.nestJwtService.sign(payload);
  }

  /**
   * Verify and decode JWT token
   */
  verifyToken(token: string): any {
    return this.nestJwtService.verify(token);
  }

  /**
   * Decode JWT token without verification (for debugging)
   */
  decodeToken(token: string): any {
    return this.nestJwtService.decode(token);
  }
}
