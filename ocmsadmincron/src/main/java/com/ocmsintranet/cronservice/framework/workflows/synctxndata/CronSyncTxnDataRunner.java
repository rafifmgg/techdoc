package com.ocmsintranet.cronservice.framework.workflows.synctxndata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

/**
 * Runner to execute CronSyncTxnData manually
 * Enable @Component to run on application startup
 */
// @Component
public class CronSyncTxnDataRunner implements CommandLineRunner {

    private final CronSyncTxnData cronSyncTxnData;

    @Autowired
    public CronSyncTxnDataRunner(CronSyncTxnData cronSyncTxnData) {
        this.cronSyncTxnData = cronSyncTxnData;
    }

    @Override
    public void run(String... args) {
        System.out.println("Running CronSyncTxnData manually...");
        cronSyncTxnData.syncTransactionData();
        System.out.println("Finished running CronSyncTxnData");
    }
}
