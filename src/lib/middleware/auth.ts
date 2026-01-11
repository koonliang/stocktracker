import { jwtService } from '../auth/jwt.service';
import { userService } from '../user/user.service';
import { UnauthorizedException } from '../common/exceptions';
import { User } from '../database/types';

export interface AuthenticatedUser {
  userId: number;
  email: string;
  user: User;
}

/**
 * Authenticate request by verifying JWT token in Authorization header
 * @param request - Next.js Request object
 * @returns Authenticated user data
 * @throws UnauthorizedException if token is missing or invalid
 */
export async function authenticateRequest(
  request: Request,
): Promise<AuthenticatedUser> {
  const authHeader = request.headers.get('authorization');

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    throw new UnauthorizedException('No token provided');
  }

  const token = authHeader.split(' ')[1];

  try {
    const payload = jwtService.verifyToken(token);

    // Ensure payload is an object, not a string
    if (typeof payload === 'string') {
      throw new UnauthorizedException('Invalid token format');
    }

    const email = payload.sub as string;

    // Fetch user from database to ensure they still exist and are enabled
    const user = await userService.findByEmail(email);

    if (!user) {
      throw new UnauthorizedException('User not found');
    }

    if (!user.enabled) {
      throw new UnauthorizedException('User account is disabled');
    }

    return {
      userId: Number(user.id),
      email: user.email,
      user,
    };
  } catch (error) {
    if (error instanceof UnauthorizedException) {
      throw error;
    }
    throw new UnauthorizedException('Invalid token');
  }
}

/**
 * Extract user from request without throwing error
 * Useful for optional authentication
 * @param request - Next.js Request object
 * @returns Authenticated user data or null
 */
export async function getOptionalUser(
  request: Request,
): Promise<AuthenticatedUser | null> {
  try {
    return await authenticateRequest(request);
  } catch {
    return null;
  }
}
