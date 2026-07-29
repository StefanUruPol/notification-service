package com.sagant.notifications.channel;

import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationChannel;


public interface ChannelDispatcher {

    NotificationChannel channel();

    void send(Notification notification) throws Exception;
}
