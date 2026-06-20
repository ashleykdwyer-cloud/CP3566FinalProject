package com.example.fraud.seed;

import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GIVEN code — reads the shared <b>fraud-data.xlsx</b> your instructor gives the class and loads it
 * into the database. You do NOT edit this; it is the same kind of plumbing as the generator.
 *
 * It <b>streams</b> the workbook one row at a time (via excel-streaming-reader, which wraps Apache
 * POI), so even a <b>million-row</b> file loads with low, bounded memory — it never holds the whole
 * sheet in RAM. Rows go into the database with a batched PreparedStatement (fast, injection-safe).
 *
 * Expected sheet (first sheet) with a header row naming the columns (any order):
 *
 *   account_no | holder_name | amount | currency | counterparty | country | occurred_at
 *
 *   - account_no   groups rows into accounts (one accounts row per distinct account_no)
 *   - holder_name  optional (defaults to account_no if the column is absent)
 *   - amount       a number  ·  currency e.g. CAD
 *   - occurred_at  an Excel date cell, or text like "2025-09-01 08:14" / "2025-09-01T08:14:00"
 */
public class ExcelDataLoader {

    private static final int BATCH = 5_000;
    private static final DataFormatter FMT = new DataFormatter();

    /** Stream {@code xlsx} and batch-insert its accounts + transactions. @return the transaction count. */
    public int load(Connection con, File xlsx) throws Exception {
        Map<String, Integer> col = null;          // header name -> column index (set from the first row)
        Map<String, Long> accountId = new HashMap<>();  // account_no -> generated id (small: one per account)
        int n = 0, pending = 0;

        try (InputStream in = new FileInputStream(xlsx);
             Workbook wb = StreamingReader.builder().rowCacheSize(256).bufferSize(16384).open(in);
             PreparedStatement insAccount = con.prepareStatement(
                     "INSERT INTO accounts(holder_name, account_no, country) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insTxn = con.prepareStatement(
                     "INSERT INTO transactions(account_id, amount, currency, counterparty, country, occurred_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)")) {

            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (col == null) {                 // the first row is the header
                    col = headerIndex(row);
                    require(col, "account_no", "amount", "currency", "counterparty", "country", "occurred_at");
                    continue;
                }
                String no = str(row, col.get("account_no"));
                if (no.isEmpty()) continue;        // skip blank rows

                // create the account the first time we see its account_no, and remember its id
                Long acctId = accountId.get(no);
                if (acctId == null) {
                    String holder = col.containsKey("holder_name") ? str(row, col.get("holder_name")) : no;
                    insAccount.setString(1, holder.isEmpty() ? no : holder);
                    insAccount.setString(2, no);
                    String country = str(row, col.get("country"));
                    insAccount.setString(3, country.isEmpty() ? "CA" : country);
                    insAccount.executeUpdate();
                    try (ResultSet keys = insAccount.getGeneratedKeys()) { keys.next(); acctId = keys.getLong(1); }
                    accountId.put(no, acctId);
                }

                insTxn.setLong(1, acctId);
                insTxn.setBigDecimal(2, amount(row, col.get("amount")));
                insTxn.setString(3, str(row, col.get("currency")));
                insTxn.setString(4, str(row, col.get("counterparty")));
                insTxn.setString(5, str(row, col.get("country")));
                insTxn.setTimestamp(6, Timestamp.valueOf(when(row, col.get("occurred_at"))));
                insTxn.addBatch();
                n++;
                if (++pending == BATCH) { insTxn.executeBatch(); con.commit(); pending = 0; }
            }
            if (pending > 0) { insTxn.executeBatch(); con.commit(); }
        }
        return n;
    }

    // ---- helpers ----

    /** Map lower-cased header names to their column index, so column order doesn't matter. */
    private static Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Cell cell : header) {
            String name = FMT.formatCellValue(cell).trim().toLowerCase().replace(' ', '_');
            if (!name.isEmpty()) m.put(name, cell.getColumnIndex());
        }
        return m;
    }

    private static void require(Map<String, Integer> col, String... names) {
        for (String n : names)
            if (!col.containsKey(n))
                throw new IllegalStateException("fraud-data.xlsx is missing the '" + n + "' column. Expected: "
                        + "account_no, holder_name, amount, currency, counterparty, country, occurred_at");
    }

    private static String str(Row row, Integer col) {
        if (col == null) return "";
        Cell c = row.getCell(col);
        return c == null ? "" : FMT.formatCellValue(c).trim();
    }

    private static BigDecimal amount(Row row, Integer col) {
        Cell c = col == null ? null : row.getCell(col);
        if (c == null) return BigDecimal.ZERO;
        if (c.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(c.getNumericCellValue());
        String s = FMT.formatCellValue(c).replace(",", "").replace("$", "").trim();
        return s.isEmpty() ? BigDecimal.ZERO : new BigDecimal(s);
    }

    private static final DateTimeFormatter[] DATETIME_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
    };

    private static LocalDateTime when(Row row, Integer col) {
        Cell c = col == null ? null : row.getCell(col);
        if (c == null) throw new IllegalStateException("a row is missing occurred_at");
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c))
            return c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        String s = FMT.formatCellValue(c).trim();
        for (DateTimeFormatter f : DATETIME_FORMATS) {
            try { return LocalDateTime.parse(s, f); } catch (Exception ignore) { }
        }
        try { return LocalDate.parse(s).atStartOfDay(); } catch (Exception ignore) { }
        throw new IllegalStateException("Could not read occurred_at '" + s + "' (use e.g. 2025-09-01 08:14).");
    }
}
