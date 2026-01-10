import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy } from 'passport-local';
import * as bcrypt from 'bcrypt';
import { UserService } from '../../user/user.service';

@Injectable()
export class LocalStrategy extends PassportStrategy(Strategy) {
  constructor(private userService: UserService) {
    super({
      usernameField: 'email', // Use email instead of username
      passwordField: 'password',
    });
  }

  async validate(email: string, password: string): Promise<any> {
    // Find user by email
    const user = await this.userService.findByEmail(email);
    if (!user) {
      throw new UnauthorizedException('Invalid email or password');
    }

    // Verify password
    if (!user.password) {
      throw new UnauthorizedException('Invalid email or password');
    }

    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid email or password');
    }

    // Check if user is enabled
    if (!user.enabled) {
      throw new UnauthorizedException('User account is disabled');
    }

    // Return user (will be attached to request.user)
    return {
      userId: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
    };
  }
}
