import * as jwt from 'jsonwebtoken';
import { User } from '../database/types';

export class JwtService {
  private readonly secret: string;
  private readonly expiresIn: string;

  constructor() {
    this.secret = process.env.JWT_SECRET || 'default-secret-change-in-production';
    const expiration = process.env.JWT_EXPIRATION || '86400000'; // 24 hours in ms
    // Convert milliseconds to seconds for JWT (jwt library expects seconds)
    this.expiresIn = `${Math.floor(parseInt(expiration) / 1000)}s`;
  }

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
      userId: Number(user.id), // Include user ID for easier lookup
    };

    return jwt.sign(payload, this.secret, {
      expiresIn: this.expiresIn,
    });
  }

  /**
   * Verify and decode JWT token
   */
  verifyToken(token: string): jwt.JwtPayload | string {
    try {
      return jwt.verify(token, this.secret);
    } catch {
      throw new Error('Invalid token');
    }
  }

  /**
   * Decode JWT token without verification (for debugging)
   */
  decodeToken(token: string): jwt.JwtPayload | string | null {
    return jwt.decode(token);
  }
}

// Singleton instance
export const jwtService = new JwtService();
