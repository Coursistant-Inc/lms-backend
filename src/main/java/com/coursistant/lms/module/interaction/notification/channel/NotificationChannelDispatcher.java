package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.event.NotificationEvent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class NotificationChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationChannelDispatcher.class);

    @Resource
    private NotificationChannelRouter notificationChannelRouter;

    @Resource
    private InAppNotificationChannel inAppNotificationChannel;

    @Resource
    private EmailQueueChannel emailQueueChannel;

    public DispatchOutcome dispatch(NotificationEvent event, List<Integer> recipientIds) {
        Set<NotificationChannel> channels = notificationChannelRouter.channelsFor(event);
        List<ChannelPersistResult> results = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            try {
                results.add(persist(channel, event, recipientIds));
            } catch (Exception e) {
                log.warn("Channel persist threw: channel={} type={} error={}",
                        channel, event != null ? event.getEventType() : null, e.getMessage());
                results.add(new ChannelPersistResult(channel, false, 0, e.getMessage()));
            }
        }
        return new DispatchOutcome(results);
    }

    private ChannelPersistResult persist(NotificationChannel channel, NotificationEvent event,
                                         List<Integer> recipientIds) {
        return switch (channel) {
            case IN_APP -> inAppNotificationChannel.persist(event, recipientIds);
            case IMMEDIATE_EMAIL -> emailQueueChannel.persistImmediate(event, recipientIds);
            case DAILY_DIGEST -> emailQueueChannel.persistDigest(event, recipientIds);
        };
    }
}
