package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;

public record ChannelPersistResult(NotificationChannel channel, boolean persisted, int rows, String failureReason) {
}
