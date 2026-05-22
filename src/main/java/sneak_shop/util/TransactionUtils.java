package sneak_shop.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

public class TransactionUtils {

    public static void afterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runnable.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

    public static void afterCompletion(Consumer<Integer> consumer) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            consumer.accept(TransactionSynchronization.STATUS_UNKNOWN);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                consumer.accept(status);
            }
        });
    }

}
