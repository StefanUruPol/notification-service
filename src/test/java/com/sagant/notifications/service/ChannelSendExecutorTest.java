package com.sagant.notifications.service;

import com.sagant.notifications.channel.ChannelDispatcher;
import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationChannel;
import com.sagant.notifications.entity.NotificationPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig
class ChannelSendExecutorTest {

    @Configuration
    @EnableRetry
    static class TestConfig {
        @Bean
        FlakyChannelDispatcher flakyChannelDispatcher() {
            return new FlakyChannelDispatcher();
        }

        @Bean
        ChannelSendExecutor channelSendExecutor(FlakyChannelDispatcher flaky) {
            return new ChannelSendExecutor(List.of(flaky));
        }
    }

    @Autowired
    private ChannelSendExecutor channelSendExecutor;

    @Autowired
    private FlakyChannelDispatcher flakyChannelDispatcher;

    @BeforeEach
    void resetFlakyDispatcher() {
        flakyChannelDispatcher.reset();
    }

    private Notification sampleNotification() {
        return Notification.builder()
                .recipient("dummy")
                .channel(NotificationChannel.LOG)
                .subject("subject")
                .body("body")
                .priority(NotificationPriority.MEDIUM)
                .build();
    }

    @Test
    void reintentaHastaTenerExitoDentroDelLimiteDeIntentos() throws Exception {
        flakyChannelDispatcher.failNextCalls(2);

        channelSendExecutor.sendWithRetry(sampleNotification());

        assertThat(flakyChannelDispatcher.getCallCount()).isEqualTo(3);
    }

    @Test
    void propagaLaExcepcionSiSeAgotanLosIntentos() {
        flakyChannelDispatcher.failNextCalls(Integer.MAX_VALUE);

        assertThatThrownBy(() -> channelSendExecutor.sendWithRetry(sampleNotification()))
                .isInstanceOf(RuntimeException.class);

        assertThat(flakyChannelDispatcher.getCallCount()).isEqualTo(3);
    }

    static class FlakyChannelDispatcher implements ChannelDispatcher {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private int failuresRemaining = 0;

        void failNextCalls(int n) {
            this.failuresRemaining = n;
        }

        void reset() {
            this.callCount.set(0);
            this.failuresRemaining = 0;
        }

        int getCallCount() {
            return callCount.get();
        }

        @Override
        public NotificationChannel channel() {
            return NotificationChannel.LOG;
        }

        @Override
        public void send(Notification notification) {
            callCount.incrementAndGet();
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new RuntimeException("Fallo simulado de destino externo");
            }
        }
    }
}