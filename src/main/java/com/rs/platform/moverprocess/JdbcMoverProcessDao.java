package com.rs.platform.moverprocess;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.util.function.Function;

import static java.lang.String.format;

class JdbcMoverProcessDao implements MoverProcessDao {
    private static String insertCommandSql;
    private static String latestCommandSql;
    private static String removeCommandSql;
    private static String lockTrackingKeySql;
    private static String updateTrackingKeySql;
    private static String upsertTrackingKeySql;
    private static String deleteTrackingKeySql;
    private static String createTableTracksIfNotExists;
    private static String createTableTasksLogIfNotExists;

    private final JdbcTemplate jdbcTemplate;
    private final CommandSerDes commandSerDes;
    private final Function<String, Class<? extends Command>> commandTypeResolver;

    JdbcMoverProcessDao(DataSource h2DataSourceModePostgreSql, CommandSerDes commandSerDes, String tasksLogTableName, String tracksTableName) {
        this.jdbcTemplate = new JdbcTemplate(h2DataSourceModePostgreSql);
        this.prepareSqlTemplates(tasksLogTableName, tracksTableName);
        this.createSchemaIfNotExists();

        this.commandSerDes = commandSerDes;
        this.commandTypeResolver = new CommandTypeResolverFunction();
    }

    @Override
    public void addCommand(@Nonnull Command command) {
        jdbcTemplate.update(insertCommandSql,
                command.getId(), commandSerDes.serialize(command), command.getTrackingKey(), command.getClass().getCanonicalName());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends Command> T latestCommandByTrackingKey(@Nonnull String trackingKey) {
        return jdbcTemplate.query(latestCommandSql, rs -> {
            if (rs.next()) {
                Class<T> taskType = (Class<T>) commandTypeResolver.apply(rs.getString("task_type"));
                return commandSerDes.deserialize(rs.getString("payload_json"), taskType);
            } else {
                return null;
            }
        }, trackingKey);
    }

    @Override
    public void removeCommand(@Nonnull String trackingKey) {
        jdbcTemplate.update(removeCommandSql, trackingKey);
    }

    @Nullable
    @Override
    public String lockTrackingKey() {
        return DataAccessUtils.singleResult(jdbcTemplate.queryForList(lockTrackingKeySql, String.class));
    }

    @Override
    public void updateTrackingKey(@Nonnull String trackingKey) {
        jdbcTemplate.update(updateTrackingKeySql, trackingKey);
    }

    @Override
    public void createOrUpdateTrackingKey(@Nonnull String trackingKey) {
        jdbcTemplate.update(upsertTrackingKeySql, trackingKey);
    }

    @Override
    public void removeByTrackingKey(String trackingKey) {
        jdbcTemplate.update(deleteTrackingKeySql, trackingKey);
    }

    private void prepareSqlTemplates(String tasksLogTableName, String tracksTableName) {
        insertCommandSql =
                format("INSERT INTO %s (task_id, payload_json, task_key, task_type) VALUES (?, ?, ?, ?)", tasksLogTableName);

        latestCommandSql =
                format("SELECT * FROM %s WHERE internal_id = ( " +
                        "   SELECT internal_id FROM %s " +
                        "   WHERE task_key = ? ORDER BY internal_id LIMIT 1" +
                        ") FOR UPDATE", tasksLogTableName, tasksLogTableName);

        removeCommandSql =
                format("DELETE FROM %s WHERE task_id = ?", tasksLogTableName);

        lockTrackingKeySql =
                format("SELECT task_key FROM %s ORDER BY last_updated_at LIMIT 1 FOR UPDATE", tracksTableName);

        updateTrackingKeySql =
                format("UPDATE %s SET last_updated_at = now() WHERE task_key = ?", tracksTableName);

        upsertTrackingKeySql =
                format("INSERT INTO %s (task_key, created_at, last_updated_at) " +
                        "VALUES (?, now(), now()) ON CONFLICT DO NOTHING", tracksTableName);

        deleteTrackingKeySql =
                format("DELETE FROM %s WHERE task_key = ?", tracksTableName);

        createTableTracksIfNotExists = format("CREATE TABLE IF NOT EXISTS %s " +
                "(" +
                "    task_key        VARCHAR(1000) PRIMARY KEY, " +
                "    last_updated_at TIMESTAMP DEFAULT now(), " +
                "    created_at      TIMESTAMP DEFAULT now() " +
                ")", tracksTableName);

        createTableTasksLogIfNotExists = format("CREATE TABLE IF NOT EXISTS %s " +
                "(" +
                "    internal_id  SERIAL PRIMARY KEY, " +
                "    task_id      VARCHAR(100)  NOT NULL UNIQUE, " +
                "    task_type    VARCHAR(2000), " +
                "    headers_json TEXT, " +
                "    payload_json TEXT, " +
                "    task_key     VARCHAR(1000) NOT NULL, " +
                "    created_at   TIMESTAMP DEFAULT now() " +
                ")", tasksLogTableName);
    }

    @Transactional
    void createSchemaIfNotExists() {
        jdbcTemplate.execute(createTableTracksIfNotExists);
        jdbcTemplate.execute(createTableTasksLogIfNotExists);
    }
}
