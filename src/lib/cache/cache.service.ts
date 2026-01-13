/**
 * Cache service for portfolio and performance data
 *
 * Cache Strategy:
 * - Portfolio: 2 minutes TTL (frequently updated with live prices)
 * - Performance History: 10 minutes TTL (less frequently updated)
 * - Evict on transaction mutations (create, update, delete)
 *
 * Using simple in-memory Map for caching (similar to Java's Caffeine)
 */

interface CacheEntry<T> {
  value: T;
  expiresAt: number;
}

export class CacheService {
  private cache: Map<string, CacheEntry<unknown>> = new Map();
  private cleanupInterval: NodeJS.Timeout | null = null;

  constructor() {
    console.log('Cache initialized successfully');
    // Clean up expired entries every minute
    this.cleanupInterval = setInterval(() => this.cleanupExpired(), 60000);
  }

  /**
   * Clean up expired cache entries
   */
  private cleanupExpired(): void {
    const now = Date.now();
    const keysToDelete: string[] = [];

    for (const [key, entry] of this.cache.entries()) {
      if (now > entry.expiresAt) {
        keysToDelete.push(key);
      }
    }

    for (const key of keysToDelete) {
      this.cache.delete(key);
    }

    if (keysToDelete.length > 0) {
      console.log(`Cleaned up ${keysToDelete.length} expired cache entries`);
    }
  }

  /**
   * Get value from cache
   */
  async get<T>(key: string): Promise<T | undefined> {
    try {
      const entry = this.cache.get(key);

      if (!entry) {
        return undefined;
      }

      // Check if expired
      if (Date.now() > entry.expiresAt) {
        this.cache.delete(key);
        return undefined;
      }

      return entry.value as T;
    } catch (error) {
      console.error(`Cache get error for key ${key}:`, error);
      return undefined;
    }
  }

  /**
   * Set value in cache with TTL (in milliseconds)
   */
  async set<T>(key: string, value: T, ttl: number = 120000): Promise<void> {
    try {
      const expiresAt = Date.now() + ttl;
      this.cache.set(key, { value, expiresAt });
    } catch (error) {
      console.error(`Cache set error for key ${key}:`, error);
    }
  }

  /**
   * Delete a specific key from cache
   */
  async del(key: string): Promise<void> {
    try {
      this.cache.delete(key);
    } catch (error) {
      console.error(`Cache delete error for key ${key}:`, error);
    }
  }

  /**
   * Delete all cache entries for a user
   */
  async evictUserCache(userId: number): Promise<void> {
    const portfolioKey = `portfolio:${userId}`;
    const performanceKeys = [
      `performance:${userId}:7d`,
      `performance:${userId}:1mo`,
      `performance:${userId}:3mo`,
      `performance:${userId}:ytd`,
      `performance:${userId}:1y`,
      `performance:${userId}:all`,
    ];

    await Promise.all([
      this.del(portfolioKey),
      ...performanceKeys.map((key) => this.del(key)),
    ]);

    console.log(`Evicted cache for user ${userId}`);
  }

  /**
   * Clear all cache entries
   */
  async reset(): Promise<void> {
    try {
      this.cache.clear();
      console.log('Cache reset successfully');
    } catch (error) {
      console.error('Cache reset error:', error);
    }
  }

  /**
   * Get cache stats
   */
  getStats(): { size: number; keys: string[] } {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys()),
    };
  }

  /**
   * Cleanup on shutdown
   */
  destroy(): void {
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval);
      this.cleanupInterval = null;
    }
    this.cache.clear();
  }
}

// Singleton instance
export const cacheService = new CacheService();
