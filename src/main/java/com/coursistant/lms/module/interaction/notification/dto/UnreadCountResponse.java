package com.coursistant.lms.module.interaction.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UnreadCountResponse", description = "Unread in-app notification count")
public class UnreadCountResponse {

    @Schema(description = "Unread count for the caller", example = "3")
    private long unreadCount;

    public UnreadCountResponse() {
    }

    public UnreadCountResponse(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
