package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class FakeNotificationEmailSender implements NotificationEmailSender {

    private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    private Function<EmailMessage, EmailSendResult> behavior = msg -> EmailSendResult.dryRun("fake-" + sent.size());

    public void failRetryable(FailureCategory category, String error) {
        behavior = msg -> EmailSendResult.retryable(category, error);
    }

    public void failPermanent(FailureCategory category, String error) {
        behavior = msg -> EmailSendResult.permanent(category, error);
    }

    public void succeed(String providerMessageId) {
        behavior = msg -> EmailSendResult.sent(providerMessageId);
    }

    public List<EmailMessage> messages() {
        return new ArrayList<>(sent);
    }

    public void reset() {
        sent.clear();
        behavior = msg -> EmailSendResult.dryRun("fake-" + sent.size());
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        sent.add(message);
        return behavior.apply(message);
    }
}
