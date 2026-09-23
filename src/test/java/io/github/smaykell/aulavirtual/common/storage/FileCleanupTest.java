package io.github.smaykell.aulavirtual.common.storage;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class FileCleanupTest {

    private static final String KEY = "courses/algebra/materials/x/tema-1.pdf";

    @Mock
    private FileStorage fileStorage;

    @BeforeEach
    void startTransaction() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void endTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void nothing_is_deleted_before_the_commit() {
        new FileCleanup(fileStorage).deleteAfterCommit(KEY);

        verify(fileStorage, never()).delete(KEY);
    }

    @Test
    void the_file_is_deleted_once_the_transaction_commits() {
        new FileCleanup(fileStorage).deleteAfterCommit(KEY);

        commit();

        verify(fileStorage).delete(KEY);
    }

    @Test
    void a_rolled_back_transaction_keeps_the_file() {
        new FileCleanup(fileStorage).deleteAfterCommit(KEY);

        synchronizations().forEach(sync ->
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(fileStorage, never()).delete(KEY);
    }

    @Test
    void a_failed_delete_does_not_reach_the_committed_request() {
        doThrow(new IllegalStateException("sin red")).when(fileStorage).delete(KEY);
        new FileCleanup(fileStorage).deleteAfterCommit(KEY);

        commit();

        verify(fileStorage).delete(KEY);
    }

    private static void commit() {
        synchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private static List<TransactionSynchronization> synchronizations() {
        return TransactionSynchronizationManager.getSynchronizations();
    }
}
