package com.vrris.pacs.support;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Test chay Flyway tren database/migrations nhung DUNG o target (src/test/resources/application.yml,
 * hien la 6) de KHONG nap du lieu demo V7/V8 - test tu seed du lieu toi thieu. Cac migration SCHEMA
 * dat sau seed (V9+, vi du row_version/daily_sequence) van can cho model hien tai, nen sau
 * flyway.migrate() lop nay chay tiep cac file V&lt;n&gt;__*.sql co n &gt; target va KHONG thuoc
 * SEED_VERSIONS, doc thang tu database/migrations (khong chep migration sang thu muc test - van
 * mot nguon duy nhat).
 *
 * Them migration seed moi thi them so phien ban vao SEED_VERSIONS. Bang
 * test_extra_migration_history ghi lai file da chay de khong chay lai khi container MySQL dung
 * chung giua nhieu Spring context.
 */
@Configuration
public class SchemaMigrationsAfterSeedConfig {

    /** Migration chi chua du lieu demo - bo qua trong test. */
    static final Set<Integer> SEED_VERSIONS = Set.of(7, 8);

    private static final Pattern VERSIONED_FILE = Pattern.compile("^V(\\d+)__.+\\.sql$");
    private static final Path MIGRATIONS_DIR = Path.of("..", "database", "migrations");

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            flyway.migrate();
            applySchemaMigrationsAfterTarget(flyway, dataSource);
        };
    }

    private void applySchemaMigrationsAfterTarget(Flyway flyway, DataSource dataSource) {
        MigrationVersion target = flyway.getConfiguration().getTarget();
        int targetVersion = target == null || target.getVersion() == null
                ? Integer.MAX_VALUE : Integer.parseInt(target.getVersion());

        try (Connection connection = dataSource.getConnection()) {
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS test_extra_migration_history (version INT PRIMARY KEY)");
            }
            for (Path file : listMigrations()) {
                int version = versionOf(file);
                if (version <= targetVersion || SEED_VERSIONS.contains(version) || alreadyApplied(connection, version)) {
                    continue;
                }
                ScriptUtils.executeSqlScript(connection,
                        new EncodedResource(new FileSystemResource(file), StandardCharsets.UTF_8));
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO test_extra_migration_history (version) VALUES (?)")) {
                    ps.setInt(1, version);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong chay duoc migration schema sau seed cho test", ex);
        }
    }

    private static List<Path> listMigrations() {
        try (Stream<Path> files = Files.list(MIGRATIONS_DIR)) {
            return files.filter(p -> VERSIONED_FILE.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(SchemaMigrationsAfterSeedConfig::versionOf))
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Khong doc duoc " + MIGRATIONS_DIR.toAbsolutePath(), ex);
        }
    }

    private static int versionOf(Path file) {
        Matcher m = VERSIONED_FILE.matcher(file.getFileName().toString());
        if (!m.matches()) {
            throw new IllegalArgumentException("Ten file migration khong hop le: " + file);
        }
        return Integer.parseInt(m.group(1));
    }

    private static boolean alreadyApplied(Connection connection, int version) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM test_extra_migration_history WHERE version = ?")) {
            ps.setInt(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
