package com.rs.platform.moverprocess;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("rsp.mover")
@Configuration
@Setter
@Getter
public class MoverProcessProperties {
    private String jdbcUrl = "jdbc:h2:file:./data/mover_process";
    private String username = "mover";
    private String password = "mover";

    private String tracksTableName = "mover_tracks";
    private String tasksLogTableName = "mover_tasks_log";
    private String commandWorkerName = "mover_command_worker";
    private int pollCommandsTimeout = 2000;
    private int retryCommandsTimeout = 1000;
    private int threads = 3;

}
