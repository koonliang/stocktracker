import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { JwtStrategy } from './strategies/jwt.strategy';
import { LocalStrategy } from './strategies/local.strategy';
import { GoogleStrategy } from './strategies/google.strategy';
import { JwtService as CustomJwtService } from './jwt.service';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { OAuth2Controller } from './oauth2.controller';
import { UserModule } from '../user/user.module';

@Module({
  imports: [
    UserModule,
    PassportModule,
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => {
        const jwtSecret = configService.get<string>('JWT_SECRET');
        if (!jwtSecret) {
          throw new Error('JWT_SECRET is not defined in environment variables');
        }

        // Decode BASE64 secret to match Java's implementation
        // Java: byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        // Java: Keys.hmacShaKeyFor(keyBytes)
        const secretBuffer = Buffer.from(jwtSecret, 'base64');

        return {
          secret: secretBuffer,
          signOptions: {
            expiresIn: '24h', // 86400000ms - matches Java's jwt.expiration
            algorithm: 'HS256', // HMAC-SHA256 - matches Java's default
          },
        };
      },
    }),
  ],
  controllers: [AuthController, OAuth2Controller],
  providers: [
    JwtStrategy,
    LocalStrategy,
    GoogleStrategy,
    CustomJwtService,
    AuthService,
  ],
  exports: [CustomJwtService, AuthService, JwtModule],
})
export class AuthModule {}
