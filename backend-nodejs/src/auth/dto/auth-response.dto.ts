export class AuthResponse {
  token: string;
  type: string;
  userId: number;
  email: string;
  name: string;

  constructor(
    token: string,
    userId: bigint | number,
    email: string,
    name: string,
  ) {
    this.token = token;
    this.type = 'Bearer';
    this.userId = Number(userId); // Convert BigInt to number for JSON
    this.email = email;
    this.name = name;
  }

  static create(
    token: string,
    userId: bigint | number,
    email: string,
    name: string,
  ): AuthResponse {
    return new AuthResponse(token, userId, email, name);
  }
}
