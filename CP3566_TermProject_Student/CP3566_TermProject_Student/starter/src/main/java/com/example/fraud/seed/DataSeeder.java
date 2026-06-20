package com.example.fraud.seed;

import com.example.fraud.model.Rule;
import com.example.fraud.model.User;
import com.example.fraud.model.Watchlist;
import com.example.fraud.repo.AccountRepository;
import com.example.fraud.repo.RuleRepository;
import com.example.fraud.repo.TransactionRepository;
import com.example.fraud.repo.UserRepository;
import com.example.fraud.repo.WatchlistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * STARTER DATA — this is given to you. It does three things, and only three:
 *   1. connects to the database (the H2 datasource configured in application.properties),
 *   2. lets Hibernate declare + create the 8 tables from the @Entity classes, then
 *   3. fills accounts + transactions with ~1,000,000 SIMULATED rows — ordinary traffic plus a
 *      small, fixed, KNOWN set of planted fraud, so each rule (R1-R4) has guaranteed hits.
 *
 * What it deliberately does NOT do: it does not run any rule, raise any alert, or open any case.
 * The {@code alerts}, {@code cases} and {@code audit_log} tables are created but left EMPTY.
 * Turning the planted fraud into 16 cases is YOUR job — that is the rule engine + case workflow
 * you build on top of this data layer (see PROJECT_BRIEF.html sections 4.3-4.8).
 *
 * The bulk load uses a batched PreparedStatement on one JDBC connection (the only fast way to
 * load a million rows); values are always passed as parameters, never glued into the SQL.
 * The data is DETERMINISTIC (fixed SEED) — the same dataset every run.
 *
 * Idempotent: if the database already has transactions it skips generation, so restarts are fast.
 * To rebuild from scratch, stop the app and delete ./data.mv.db, then run again.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    // ---- tunables ----
    static final long          SEED    = 42L;                        // fixed seed -> identical data every run
    static final int           BATCH   = 5_000;                      // rows per JDBC batch / commit
    static final BigDecimal    LIMIT   = new BigDecimal("10000.00"); // R1 line and R4 ceiling
    static final LocalDateTime BASE    = LocalDateTime.of(2025, 9, 1, 0, 0); // span starts here
    static final int           SPAN_MIN = 120 * 24 * 60;             // transactions spread over 120 days

    static final String[] WATCHLIST = {"Viktor Petrov", "Shell Holdings LLC", "Nadia Volkov"};
    static final String[] ORDINARY  = {
        "Maple Grocers","City Hydro","Atlantic Telecom","QuickPay Inc","Bluewater Rentals",
        "Northstar Auto","Campus Books","Harbour Cafe","Sunrise Pharmacy","Metro Transit",
        "Riverside Gym","Lakeview Dental","Pioneer Hardware","Eastside Bakery","Summit Insurance","Delta Airlines"
    };

    // How many transactions to generate. Default 1,000,000; override in application.properties
    // (app.transaction-count=50000) for a smaller, faster run.
    @Value("${app.transaction-count:50000}")
    private int transactionCount;

    // If a file with this name exists in the project folder, it is parsed and loaded instead of
    // generating data. Your instructor gives the whole class the same fraud-data.xlsx.
    @Value("${app.data-file:fraud-data.xlsx}")
    private String dataFile;

    private final DataSource dataSource;
    private final RuleRepository ruleRepo;
    private final UserRepository userRepo;
    private final WatchlistRepository watchlistRepo;
    private final TransactionRepository transactionRepo;
    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DataSource dataSource, RuleRepository ruleRepo, UserRepository userRepo,
                      WatchlistRepository watchlistRepo, TransactionRepository transactionRepo,
                      AccountRepository accountRepo, PasswordEncoder passwordEncoder) {
        this.dataSource = dataSource;
        this.ruleRepo = ruleRepo;
        this.userRepo = userRepo;
        this.watchlistRepo = watchlistRepo;
        this.transactionRepo = transactionRepo;
        this.accountRepo = accountRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        long maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        System.out.printf("%nResources: JVM max heap = %,d MB  |  app.transaction-count = %,d (kept light for laptops).%n",
                maxHeapMb, transactionCount);
        if (transactionCount > 200_000)
            System.out.println("Note: a large transaction-count is slower and uses more disk; the default 50,000 is light and still has all 16 planted cases.");

        if (transactionRepo.count() > 0) {
            System.out.printf("%nDatabase already populated (%,d transactions) - skipping generation.%n", transactionRepo.count());
            System.out.println("To rebuild from scratch, stop the app and delete ./data.mv.db, then run again.");
            return;
        }

        // Reference data (users, watchlist, rules) through the JPA repositories.
        seedReference();

        // The transaction DATA comes from one of two places, on one batched JDBC connection:
        //   - if fraud-data.xlsx (property app.data-file) exists in the project folder, parse + load
        //     THAT — your unique, instructor-supplied dataset — via ExcelDataLoader;
        //   - otherwise generate a sample dataset, so the project still runs out of the box.
        File excel = new File(dataFile);
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            long t0 = System.nanoTime();
            if (excel.isFile()) {
                System.out.printf("%nLoading your dataset from %s ...%n", excel.getAbsolutePath());
                int n = new ExcelDataLoader().load(con, excel);
                System.out.println();
                System.out.println("================  DATASET LOADED FROM EXCEL  ================");
                System.out.printf("  transactions: %,d   loaded in %.2fs%n", n, (System.nanoTime() - t0) / 1e9);
            } else {
                int target   = transactionCount;
                int accounts  = Math.max(50, Math.min(50_000, target / 200)); // ~200 transactions/account
                System.out.printf("%nNo %s found - generating a sample dataset (~%,d transactions). "
                        + "Drop your fraud-data.xlsx in this folder to use your own.%n", dataFile, target);
                long[] ids = generateAccounts(con, accounts); con.commit();
                int n = generateTransactions(con, ids, target); con.commit();
                double genSec = (System.nanoTime() - t0) / 1e9;
                System.out.println();
                System.out.println("================  SIMULATED DATA LOADED  ================");
                System.out.printf("  accounts: %,d   transactions: %,d   (planted fraud is mixed in)%n", accounts, n);
                System.out.printf("  generated in %.2fs (%,d rows/sec)%n",
                        genSec, genSec > 0 ? (long) (n / genSec) : 0L);
            }
        }

        System.out.printf("%nThrough the JPA layer: accountRepository.count() = %,d, transactionRepository.count() = %,d%n",
                accountRepo.count(), transactionRepo.count());
        System.out.println("alerts = 0, cases = 0  ->  build the rule engine + case workflow to open the 16 cases (see PROJECT_BRIEF.html).");
        System.out.println("What to build next is listed in StudentTasks.java (and the TODO stubs in the service/ and web/ packages).");
    }

    // =========================================================================
    // 1. REFERENCE DATA — written through the repositories (Spring Data JPA)
    // =========================================================================
    private void seedReference() {
        // Passwords are stored HASHED (BCrypt), never in plain text. Demo logins: analyst1/analyst123,
        // investig1/investig123, admin1/admin123. Your login must verify with PasswordEncoder.matches().
        userRepo.saveAll(List.of(
                new User("analyst1",  "ANALYST",      passwordEncoder.encode("analyst123")),
                new User("investig1", "INVESTIGATOR", passwordEncoder.encode("investig123")),
                new User("admin1",    "ADMIN",        passwordEncoder.encode("admin123"))));

        List<Watchlist> watch = new ArrayList<>();
        for (String name : WATCHLIST) watch.add(new Watchlist(name, "Sanctions / known shell"));
        watchlistRepo.saveAll(watch);

        // Thresholds live in the rules table so an admin can change them without touching code.
        ruleRepo.saveAll(List.of(
                new Rule("R1", "Large single amount",           LIMIT, 0,    1, true),
                new Rule("R2", "Velocity (too many, too fast)", null,  60,   5, true),
                new Rule("R3", "Watchlist counterparty",        null,  0,    1, true),
                new Rule("R4", "Structuring (just under limit)",LIMIT, 1440, 3, true))); // bonus
    }

    // =========================================================================
    // 2a. ACCOUNTS  (batch insert, then read the generated ids back)
    // =========================================================================
    private long[] generateAccounts(Connection con, int accounts) throws SQLException {
        String[] first = {"Alice","Bob","Carla","Devon","Erin","Faisal","Grace","Hiro","Ivy","Jamal","Kira","Liam","Mona","Noor","Owen","Priya"};
        String[] last  = {"Tremblay","Nguyen","Mensah","Park","Walsh","Rahman","Okoye","Tanaka","Silva","Khan","Brown","Lopez","Singh","Cohen","Ali","Roy"};
        Random rnd = new Random(SEED);
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO accounts(holder_name, account_no, country) VALUES (?, ?, ?)")) {
            for (int i = 0; i < accounts; i++) {
                ps.setString(1, first[rnd.nextInt(first.length)] + " " + last[rnd.nextInt(last.length)]);
                ps.setString(2, String.format("ACCT-%06d", 1000 + i));
                ps.setString(3, "CA");
                ps.addBatch();
                if ((i + 1) % BATCH == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
        long[] ids = new long[accounts];
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM accounts ORDER BY id")) {
            int i = 0; while (rs.next()) ids[i++] = rs.getLong(1);
        }
        return ids;
    }

    // =========================================================================
    // 2b. TRANSACTIONS  (batched). Ordinary traffic + a fixed, known set of fraud.
    // =========================================================================
    private int generateTransactions(Connection con, long[] acct, int target) throws SQLException {
        Random rnd = new Random(SEED + 1);
        int perAcct = Math.max(1, target / acct.length);
        int n = 0, pending = 0;

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO transactions(account_id, amount, currency, counterparty, country, occurred_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)")) {

            // --- ordinary traffic: ~perAcct small transactions per account ---
            // Amounts stay in $5-$2000 and counterparties avoid the watchlist, so NONE of the
            // ordinary rows trip a rule. Spread each account's traffic across the span (stride +
            // jitter) so chance clusters never reach the velocity threshold. Every rule hit is
            // therefore one of the injected ones below.
            for (long id : acct) {
                long stride = Math.max(1, SPAN_MIN / perAcct);
                long half   = stride / 3;
                for (int k = 0; k < perAcct; k++) {
                    BigDecimal amount = money(rnd, 5, 2000);
                    String cp  = ORDINARY[rnd.nextInt(ORDINARY.length)];
                    long jitter = half == 0 ? 0 : rnd.nextInt((int) (2 * half + 1)) - half;
                    long minute = Math.max(0, Math.min(SPAN_MIN - 1, k * stride + jitter));
                    addTxn(ps, id, amount, "CAD", cp, "CA", BASE.plusMinutes(minute));
                    n++; if (++pending == BATCH) { ps.executeBatch(); con.commit(); pending = 0; }
                }
            }

            // --- R1 LARGE AMOUNT: 5 accounts, one over-limit transfer each ---
            for (int a = 0; a < 5; a++) {
                BigDecimal amount = new BigDecimal(15000 + rnd.nextInt(35000)).setScale(2);
                addTxn(ps, acct[a], amount, "USD", "Offshore Holdings", "KY", randTime(rnd));
                n++; if (++pending == BATCH) { ps.executeBatch(); con.commit(); pending = 0; }
            }

            // --- R3 WATCHLIST: 5 transfers to watchlisted counterparties (accounts 20-24) ---
            for (int a = 0; a < 5; a++) {
                BigDecimal amount = money(rnd, 1000, 3000);
                addTxn(ps, acct[20 + a], amount, "EUR", WATCHLIST[a % WATCHLIST.length], "RU", randTime(rnd));
                n++; if (++pending == BATCH) { ps.executeBatch(); con.commit(); pending = 0; }
            }

            // --- R2 VELOCITY: 3 accounts (10,11,12), 6 transfers inside 25 minutes each ---
            for (int a = 10; a <= 12; a++) {
                LocalDateTime burst = BASE.plusMinutes(rnd.nextInt(SPAN_MIN - 60));
                for (int m = 0; m < 6; m++) {
                    addTxn(ps, acct[a], new BigDecimal(300 + m).setScale(2), "CAD", "QuickPay Inc", "CA", burst.plusMinutes(5L * m));
                    n++; if (++pending == BATCH) { ps.executeBatch(); con.commit(); pending = 0; }
                }
            }

            // --- R4 STRUCTURING (bonus): 3 accounts (30,31,32), 4 transfers just under the limit ---
            BigDecimal[] amts = { new BigDecimal("9300.00"), new BigDecimal("9600.00"),
                                  new BigDecimal("9800.00"), new BigDecimal("9900.00") };
            int[] mins = {0, 45, 90, 150};
            for (int a = 30; a <= 32; a++) {
                LocalDateTime base = BASE.plusMinutes(rnd.nextInt(SPAN_MIN - 200));
                for (int j = 0; j < amts.length; j++) {
                    addTxn(ps, acct[a], amts[j], "CAD", "Cash Deposit", "CA", base.plusMinutes(mins[j]));
                    n++; if (++pending == BATCH) { ps.executeBatch(); con.commit(); pending = 0; }
                }
            }

            if (pending > 0) { ps.executeBatch(); con.commit(); }
        }
        return n;
    }

    private void addTxn(PreparedStatement ps, long accountId, BigDecimal amount, String ccy,
                        String counterparty, String country, LocalDateTime at) throws SQLException {
        ps.setLong(1, accountId);
        ps.setBigDecimal(2, amount);
        ps.setString(3, ccy);
        ps.setString(4, counterparty);
        ps.setString(5, country);
        ps.setTimestamp(6, Timestamp.valueOf(at));
        ps.addBatch();
    }

    // ---- helpers ----
    private static LocalDateTime randTime(Random rnd) { return BASE.plusMinutes(rnd.nextInt(SPAN_MIN)); }
    private static BigDecimal money(Random rnd, int lo, int hi) {
        long cents = lo * 100L + rnd.nextInt((hi - lo) * 100);
        return new BigDecimal(cents).movePointLeft(2);
    }
}
