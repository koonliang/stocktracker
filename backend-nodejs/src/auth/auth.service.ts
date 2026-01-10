import { Injectable } from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { UserService } from '../user/user.service';
import { JwtService } from './jwt.service';
import {
  BadRequestException,
  UnauthorizedException,
} from '../common/exceptions';
import { LoginDto, SignupDto, AuthResponse } from './dto';
import { PasswordValidator } from '../common/utils/password-validator';
import { AuthProvider, Role } from '../database/types';

@Injectable()
export class AuthService {
  constructor(
    private userService: UserService,
    private jwtService: JwtService,
  ) {}

  /**
   * Authenticate user with email and password
   * Matches Java's AuthService.login()
   */
  async login(loginDto: LoginDto): Promise<AuthResponse> {
    const { email, password } = loginDto;

    // Find user by email
    const user = await this.userService.findByEmail(email);
    if (!user) {
      throw new UnauthorizedException('Invalid email or password');
    }

    // Verify password (use bcrypt compare)
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

    // Generate JWT token
    const token = this.jwtService.generateToken(user);

    return AuthResponse.create(token, user.id, user.email, user.name);
  }

  /**
   * Register new user with email and password
   * Matches Java's UserService.registerUser()
   */
  async register(signupDto: SignupDto): Promise<AuthResponse> {
    const { name, email, password, confirmPassword } = signupDto;

    // Check if passwords match
    if (password !== confirmPassword) {
      throw new BadRequestException('Passwords do not match');
    }

    // Validate password complexity
    const passwordErrors = PasswordValidator.validate(password);
    if (passwordErrors.length > 0) {
      throw new BadRequestException(passwordErrors.join(', '));
    }

    // Check if email already exists
    const existingUser = await this.userService.existsByEmail(email);
    if (existingUser) {
      throw new BadRequestException('Email already registered');
    }

    // Hash password using bcrypt (default rounds: 10)
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create user
    const user = await this.userService.create({
      name,
      email,
      password: hashedPassword,
      auth_provider: AuthProvider.LOCAL,
      role: Role.USER,
      enabled: true,
      is_demo_account: false,
      created_at: new Date(),
      updated_at: new Date(),
    });

    // Auto-login: generate JWT token
    const token = this.jwtService.generateToken(user);

    return AuthResponse.create(token, user.id, user.email, user.name);
  }

  /**
   * Logout (stateless - client discards token)
   * Returns success message
   */
  logout(): { message: string } {
    return { message: 'Logged out successfully' };
  }
}
