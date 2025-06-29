package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import ru.mai.lessons.rpks.DbReader;
import ru.mai.lessons.rpks.model.Rule;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class DbReaderController implements DbReader {

    private final DSLContext dslContext;
    private final DataSource dataSource;
    private final ReentrantLock rulesLock = new ReentrantLock();

    // хочу явно, мьютекс-ом ошибки тут нет
    @SuppressWarnings("java:S3077")
    private volatile Rule[] currentRules = new Rule[0];
    private final long updateIntervalMillis;

    private volatile boolean running = true;
    private final Thread updateThread;


    public DbReaderController(Config config) {
        this.dataSource = createDataSource(config);
        this.dslContext = DSL.using(this.dataSource, SQLDialect.POSTGRES);
        this.updateIntervalMillis = config.getInt("application.updateIntervalSec") * 1000L;

        log.info("Performing initial rule fetch...");
        updateRulesInternal();
        log.info("Initial rule fetch complete. Count: {}", currentRules.length);

        RuleUpdaterTask updaterTask = new RuleUpdaterTask();
        this.updateThread = new Thread(updaterTask, "DbRuleUpdaterThread");
        this.updateThread.start();
    }


    private DataSource createDataSource(Config config) {
        log.info("Creating datasource...");
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(config.getString("db.jdbcUrl"));
        hikariConfig.setUsername(config.getString("db.user"));
        hikariConfig.setPassword(config.getString("db.password"));
        hikariConfig.setDriverClassName(config.getString("db.driver"));

        return new HikariDataSource(hikariConfig);
    }


    private void updateRulesInternal() {
        Rule[] newRules;
        try {
            log.debug("Attempting to fetch rules from DB...");
            newRules = getRulesFromDb();
        } catch (Exception e) {
            log.error("Error fetching rules from database", e);
            return;
        }

        rulesLock.lock();
        try {
            this.currentRules = newRules;
            log.info("Rules updated. Count: {}", newRules.length);
        } finally {
            rulesLock.unlock();
        }
    }

    public Rule[] getRulesFromDb() {
        log.debug("getting rules from DB...");

        List<Rule> rules = dslContext.select().from("deduplication_rules").fetchInto(Rule.class);
        return rules.toArray(new Rule[0]);
    }


    @Override
    public Rule[] readRulesFromDB() {
        rulesLock.lock();
        try {
            return Arrays.copyOf(this.currentRules, this.currentRules.length);
        } finally {
            rulesLock.unlock();
        }
    }


    private class RuleUpdaterTask implements Runnable {
        @Override
        public void run() {
            log.info("Rule update thread started. Update interval: {} ms", updateIntervalMillis);
            while (running) {
                try {
                    Thread.sleep(updateIntervalMillis);
                    log.info("Updating rules from DB periodically...");
                    updateRulesInternal();

                } catch (InterruptedException e) {
                    log.info("Rule update thread interrupted. Stopping updates.");
                    running = false;
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Unexpected error: ", e);
                    log.warn("Sleep after error interrupted. Stopping updates.");
                    running = false;
                    Thread.currentThread().interrupt();

                }
            }
            log.info("Rule update thread finished.");
        }
    }


    public void shutdown() {
        log.info("Shutting down DbReader...");
        running = false;

        if (updateThread != null && updateThread.isAlive()) {
            log.info("Interrupting update thread to wake it up...");
            updateThread.interrupt();
            try {
                log.info("Waiting for update thread to finish...");
                updateThread.join(TimeUnit.SECONDS.toMillis(5));
                if (updateThread.isAlive()) {
                    log.warn("Update thread did not finish within the timeout.");
                } else {
                    log.info("Update thread finished gracefully.");
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for update thread to finish.", e);
                Thread.currentThread().interrupt();
            }
        }
        if (dataSource instanceof HikariDataSource source) {
            log.info("Closing datasource...");
            source.close();
            log.info("Datasource closed.");
        } else {
            log.warn("DataSource is not an instance of HikariDataSource, cannot close it explicitly.");
        }
        log.info("DbReader shutdown complete.");
    }


    @Override
    public void close() {
        shutdown();
    }
}