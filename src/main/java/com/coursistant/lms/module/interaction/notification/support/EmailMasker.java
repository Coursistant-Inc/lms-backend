package com.coursistant.lms.module.interaction.notification.support;

public final class EmailMasker {

    private EmailMasker() {
    }

    public static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String localMasked = local.charAt(0) + "***";
        int dot = domain.lastIndexOf('.');
        String domainMasked = domain.charAt(0) + "***" + (dot > 0 ? domain.substring(dot) : "");
        return localMasked + "@" + domainMasked;
    }
}
