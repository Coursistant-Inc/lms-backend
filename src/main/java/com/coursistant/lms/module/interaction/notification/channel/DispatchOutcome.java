package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;

import java.util.List;

public record DispatchOutcome(List<ChannelPersistResult> results) {

    public boolean allPersisted() {
        if (results == null || results.isEmpty()) {
            return true;
        }
        for (ChannelPersistResult result : results) {
            if (result == null || !result.persisted()) {
                return false;
            }
        }
        return true;
    }

    public List<NotificationChannel> failedChannels() {
        return results == null ? List.of() : results.stream()
                .filter(r -> r != null && !r.persisted())
                .map(ChannelPersistResult::channel)
                .toList();
    }
}
