package com.test.pushnotification.service;

import com.test.pushnotification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmitterService {

    Map<String, SseEmitter> emitters = new HashMap<>();

    public void addEmitter(SseEmitter emitter, String username) {
        emitter.onCompletion(() -> emitters.remove(username));
        emitter.onTimeout(() -> emitters.remove(username));
        emitters.put(username, emitter);
    }

    public void pushNotification(String username, String name, String message) {
        log.info("pushing {} notification for user {}", message, username);
        Notification payload = Notification
                .builder()
                .from(name)
                .message(message)
                .build();

        try {
            emitters.get(username).send(SseEmitter
                    .event()
                    .name(username)
                    .data(payload));

        } catch (IOException e) {
            emitters.remove(username);
        }
    }
}
