package com.stocktracker.scheduler;

import com.stocktracker.entity.User;
import com.stocktracker.repository.HoldingRepository;
import com.stocktracker.repository.TransactionRepository;
import com.stocktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoAccountCleanupScheduler {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;

    /**
     * Clean up demo accounts older than 24 hours.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldDemoAccounts() {
        log.info("Starting demo account cleanup...");

        // Find demo accounts created more than 24 hours ago
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<User> oldDemoAccounts = userRepository.findByIsDemoAccountTrueAndCreatedAtBefore(cutoffTime);

        if (oldDemoAccounts.isEmpty()) {
            log.info("No demo accounts to clean up");
            return;
        }

        log.info("Found {} demo accounts to clean up", oldDemoAccounts.size());

        for (User demoUser : oldDemoAccounts) {
            try {
                // Delete in order: Holdings -> Transactions -> User (FK constraints)
                holdingRepository.deleteByUserId(demoUser.getId());
                transactionRepository.deleteByUserId(demoUser.getId());
                userRepository.delete(demoUser);

                log.info("Deleted demo account: {} (created at: {})",
                        demoUser.getEmail(), demoUser.getCreatedAt());
            } catch (Exception e) {
                log.error("Failed to delete demo account {}: {}",
                        demoUser.getEmail(), e.getMessage(), e);
            }
        }

        log.info("Demo account cleanup completed. Deleted {} accounts", oldDemoAccounts.size());
    }
}
