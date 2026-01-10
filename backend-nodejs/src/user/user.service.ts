import { Injectable } from '@nestjs/common';
import { PrismaService } from '../database/prisma.service';
import { User, UserCreateInput, UserUpdateInput } from '../database/types';

@Injectable()
export class UserService {
  constructor(private prisma: PrismaService) {}

  async findById(id: number): Promise<User | null> {
    return this.prisma.users.findUnique({
      where: { id: BigInt(id) },
    });
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.prisma.users.findUnique({
      where: { email: email.toLowerCase().trim() },
    });
  }

  async create(data: UserCreateInput): Promise<User> {
    return this.prisma.users.create({
      data: {
        ...data,
        email: data.email.toLowerCase().trim(),
      },
    });
  }

  async update(id: number, data: UserUpdateInput): Promise<User> {
    return this.prisma.users.update({
      where: { id: BigInt(id) },
      data,
    });
  }

  async delete(id: number): Promise<User> {
    return this.prisma.users.delete({
      where: { id: BigInt(id) },
    });
  }

  async existsByEmail(email: string): Promise<boolean> {
    const count = await this.prisma.users.count({
      where: { email: email.toLowerCase().trim() },
    });
    return count > 0;
  }

  async findDemoAccountsOlderThan(cutoffDate: Date): Promise<User[]> {
    return this.prisma.users.findMany({
      where: {
        is_demo_account: true,
        created_at: {
          lt: cutoffDate,
        },
      },
    });
  }
}
