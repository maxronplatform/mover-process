package org.maxron.platform.moverprocess;

import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

public final class DbMoverProcessDaoBuilder implements MoverProcessDaoBuilder {
    private DataSource dataSource;
    private String tracksTableName;
    private String tasksLogTableName;
    private CommandSerDes commandSerDes;

    @Override
    public MoverProcessDao create() {
        Validate.notNull(dataSource, "The validated dataSource is null");
        Validate.notNull(commandSerDes, "The validated serializer/deserializer is null");
        Validate.notBlank(tracksTableName, "The validated name of the tracks table (tracksTableName) cannot be empty");
        Validate.notBlank(tasksLogTableName, "The validated name of the tasks Log table (tasksLogTableName) cannot be empty");
        return new JdbcMoverProcessDao(dataSource, commandSerDes, tasksLogTableName, tracksTableName);
    }

    DbMoverProcessDaoBuilder dataSource(@Nonnull DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    DbMoverProcessDaoBuilder tracksTableName(@Nonnull String tracksTableName) {
        this.tracksTableName = tracksTableName;
        return this;
    }

    DbMoverProcessDaoBuilder tasksLogTableName(@Nonnull String tasksLogTableName) {
        this.tasksLogTableName = tasksLogTableName;
        return this;
    }

    DbMoverProcessDaoBuilder commandSerDes(@Nonnull CommandSerDes commandSerDes) {
        this.commandSerDes = commandSerDes;
        return this;
    }

    DataSource getDataSource() {
        return dataSource;
    }

    String getTasksLogTableName() {
        return tasksLogTableName;
    }

    String getTracksTableName() {
        return tracksTableName;
    }

    CommandSerDes getCommandSerializer() {
        return commandSerDes;
    }
}
