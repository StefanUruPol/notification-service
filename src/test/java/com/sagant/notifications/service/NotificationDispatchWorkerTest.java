package com.sagant.notifications.service;

import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationChannel;
import com.sagant.notifications.entity.NotificationPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationDispatchWorkerTest {

    @Mock
    private ChannelSendExecutor channelSendExecutor;

    @Mock
    private NotificationQueueService queueService;

    private NotificationDispatchWorker worker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        worker = new NotificationDispatchWorker(channelSendExecutor, queueService);
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
    void marcaComoSentCuandoElEnvioTieneExito() throws Exception {
        Notification notification = sampleNotification();
        doNothing().when(channelSendExecutor).sendWithRetry(notification);

        worker.dispatch(notification);

        verify(queueService).markSent(notification.getId());
        verify(queueService, never()).markFailed(any(), anyString());
    }

    @Test
    void marcaComoFailedCuandoElEnvioAgotaLosReintentos() throws Exception {
        Notification notification = sampleNotification();
        doThrow(new RuntimeException("destino inalcanzable"))
                .when(channelSendExecutor).sendWithRetry(notification);

        worker.dispatch(notification);

        verify(queueService).markFailed(eq(notification.getId()), eq("destino inalcanzable"));
        verify(queueService, never()).markSent(any());
    }
}
