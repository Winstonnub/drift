package com.drift.email;

import java.util.List;

public record ResendSendEmailRequest(

    String from,

    List<String> to,

    String subject,

    String html,

    String text

) {
}