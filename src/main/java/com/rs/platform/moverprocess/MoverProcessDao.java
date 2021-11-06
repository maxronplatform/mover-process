package com.rs.platform.moverprocess;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

interface MoverProcessDao {
    void addCommand(@Nonnull Command command);

    @Nullable
    <T extends Command> T latestCommandByTrackingKey(@Nonnull String trackingKey);

    void removeCommand(@Nonnull String trackingKey);

    @Nullable
    String lockTrackingKey();

    void updateTrackingKey(@Nonnull String trackingKey);

    void createOrUpdateTrackingKey(@Nonnull String trackingKey);

    void removeByTrackingKey(String trackingKey);
}
