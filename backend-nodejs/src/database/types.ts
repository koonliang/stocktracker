/**
 * Type aliases to map Prisma's generated types to our code's naming conventions
 * The database uses lowercase table names (users, transactions, holdings)
 * but our code uses PascalCase (User, Transaction, Holding)
 */
import {
  users,
  transactions,
  holdings,
  users_role,
  users_auth_provider,
  transactions_type,
  Prisma,
} from '@prisma/client';

// Model aliases
export type User = users;
export type Transaction = transactions;
export type Holding = holdings;

// Enum aliases
export type Role = users_role;
export type AuthProvider = users_auth_provider;
export type TransactionType = transactions_type;

// Enum values (for easier usage)
export const Role = {
  USER: 'USER' as users_role,
  ADMIN: 'ADMIN' as users_role,
};

export const AuthProvider = {
  LOCAL: 'LOCAL' as users_auth_provider,
  GOOGLE: 'GOOGLE' as users_auth_provider,
};

export const TransactionType = {
  BUY: 'BUY' as transactions_type,
  SELL: 'SELL' as transactions_type,
};

// Input type aliases
export type UserCreateInput = Prisma.usersCreateInput;
export type UserUpdateInput = Prisma.usersUpdateInput;
export type TransactionCreateInput = Prisma.transactionsCreateInput;
export type TransactionUpdateInput = Prisma.transactionsUpdateInput;
export type HoldingCreateInput = Prisma.holdingsCreateInput;
export type HoldingUpdateInput = Prisma.holdingsUpdateInput;
