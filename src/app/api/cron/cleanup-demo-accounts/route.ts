import { NextRequest, NextResponse } from 'next/server';
import { prisma } from '@/lib/database/prisma';

/**
 * Vercel Cron Job: Clean up demo accounts older than 24 hours
 * Scheduled to run daily at 2 AM via vercel.json configuration
 *
 * Authorization: Uses Vercel Cron Secret for security
 */
export async function GET(request: NextRequest) {
  try {
    // Verify cron secret to prevent unauthorized access
    const authHeader = request.headers.get('authorization');
    const cronSecret = process.env.CRON_SECRET;

    if (cronSecret && authHeader !== `Bearer ${cronSecret}`) {
      console.error('Unauthorized cron job access attempt');
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    console.log('Starting demo account cleanup...');

    // Calculate cutoff time (24 hours ago)
    const cutoffDate = new Date();
    cutoffDate.setHours(cutoffDate.getHours() - 24);

    // Find demo accounts older than 24 hours
    const demoAccounts = await prisma.users.findMany({
      where: {
        is_demo_account: true,
        created_at: {
          lt: cutoffDate,
        },
      },
      select: {
        id: true,
        email: true,
        created_at: true,
      },
    });

    console.log(`Found ${demoAccounts.length} demo accounts to delete`);

    let deletedCount = 0;
    const errors: string[] = [];

    // Delete each demo account and its related data
    for (const account of demoAccounts) {
      try {
        // Delete transactions (cascade will handle holdings via DB constraints if set)
        await prisma.transactions.deleteMany({
          where: { user_id: account.id },
        });

        // Delete holdings
        await prisma.holdings.deleteMany({
          where: { user_id: account.id },
        });

        // Delete user
        await prisma.users.delete({
          where: { id: account.id },
        });

        deletedCount++;
        console.log(
          `Deleted demo account: ${account.email} (created: ${account.created_at.toISOString()})`,
        );
      } catch (error) {
        const errorMsg = `Failed to delete account ${account.email}: ${error instanceof Error ? error.message : 'Unknown error'}`;
        console.error(errorMsg);
        errors.push(errorMsg);
      }
    }

    console.log(`Demo account cleanup completed: ${deletedCount}/${demoAccounts.length} deleted`);

    return NextResponse.json(
      {
        success: true,
        message: 'Demo account cleanup completed',
        data: {
          found: demoAccounts.length,
          deleted: deletedCount,
          errors: errors.length > 0 ? errors : undefined,
        },
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Demo account cleanup error:', error);

    return NextResponse.json(
      {
        success: false,
        message: 'Failed to clean up demo accounts',
        error: error instanceof Error ? error.message : 'Unknown error',
      },
      { status: 500 },
    );
  }
}
