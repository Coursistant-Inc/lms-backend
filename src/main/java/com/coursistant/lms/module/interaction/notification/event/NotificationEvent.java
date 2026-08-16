package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationEvent {

    private String eventId;
    private NotificationType eventType;
    private String eventKey;
    private int eventVersion = 1;
    private Integer tenantId;
    private Integer courseId;
    private Integer actorUserId;
    private RecipientMode recipientMode;
    private List<Integer> recipientIds = new ArrayList<>();
    private SubjectType subjectType;
    private Integer subjectId;
    private String message;
    private String deepLink;
    private LocalDateTime occurredAt;
    private Map<String, String> templateVars = new LinkedHashMap<>();
    private Set<NotificationChannel> channelPolicy;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public NotificationType getEventType() {
        return eventType;
    }

    public void setEventType(NotificationType eventType) {
        this.eventType = eventType;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(int eventVersion) {
        this.eventVersion = eventVersion;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Integer actorUserId) {
        this.actorUserId = actorUserId;
    }

    public RecipientMode getRecipientMode() {
        return recipientMode;
    }

    public void setRecipientMode(RecipientMode recipientMode) {
        this.recipientMode = recipientMode;
    }

    public List<Integer> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<Integer> recipientIds) {
        this.recipientIds = recipientIds == null ? new ArrayList<>() : recipientIds;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Map<String, String> getTemplateVars() {
        return templateVars;
    }

    public void setTemplateVars(Map<String, String> templateVars) {
        this.templateVars = templateVars == null ? new LinkedHashMap<>() : templateVars;
    }

    public Set<NotificationChannel> getChannelPolicy() {
        return channelPolicy;
    }

    public void setChannelPolicy(Set<NotificationChannel> channelPolicy) {
        this.channelPolicy = channelPolicy;
    }
}
