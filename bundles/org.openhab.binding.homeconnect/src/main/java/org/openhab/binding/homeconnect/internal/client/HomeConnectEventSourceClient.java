/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.homeconnect.internal.client;

import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.API_BASE_URL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.API_SIMULATOR_BASE_URL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.openhab.binding.homeconnect.internal.client.exception.AuthorizationException;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.client.listener.HomeConnectEventListener;
import org.openhab.binding.homeconnect.internal.client.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;

/**
 * Server-Sent-Events client for Home Connect API.
 *
 * @author Jonas Brüstel - Initial contribution
 *
 */
@NonNullByDefault
public class HomeConnectEventSourceClient {

    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final int SSE_REQUEST_READ_TIMEOUT = 90;
    private static final String ACCEPT = "Accept";
    private static final int EVENT_QUEUE_SIZE = 150;

    private final String apiUrl;
    private final EventSource.Factory eventSourceFactory;
    private final OAuthClientService oAuthClientService;
    private final Map<HomeConnectEventListener, EventSource> eventSourceConnections;
    private final ScheduledExecutorService scheduler;
    private final Queue<Event> eventQueue;

    private final Logger logger;

    public HomeConnectEventSourceClient(OAuthClientService oAuthClientService, boolean simulated,
            ScheduledExecutorService scheduler, @Nullable List<Event> eventHistory) {
        this.scheduler = scheduler;
        this.oAuthClientService = oAuthClientService;

        apiUrl = simulated ? API_SIMULATOR_BASE_URL : API_BASE_URL;
        eventSourceFactory = EventSources.createFactory(OkHttpHelper.builder()
                .readTimeout(SSE_REQUEST_READ_TIMEOUT, TimeUnit.SECONDS).retryOnConnectionFailure(true).build());
        eventSourceConnections = new HashMap<>();
        eventQueue = QueueUtils.synchronizedQueue(new CircularFifoQueue<>(EVENT_QUEUE_SIZE));
        if (eventHistory != null) {
            eventQueue.addAll(eventHistory);
        }
        logger = LoggerFactory.getLogger(HomeConnectEventSourceClient.class);
    }

    /**
     * Register {@link HomeConnectEventListener} to receive events by Home Conncet API. This helps to reduce the
     * amount of request you would usually need to update all channels.
     *
     * Checkout rate limits of the API at. https://developer.home-connect.com/docs/general/ratelimiting
     *
     * @param eventListener appliance event listener
     * @throws CommunicationException API communication exception
     * @throws AuthorizationException oAuth authorization exception
     */
    public synchronized void registerEventListener(final String haId, final HomeConnectEventListener eventListener)
            throws CommunicationException, AuthorizationException {
        logger.debug("Register event listener for '{}': {}", haId, eventListener);

        if (!eventSourceConnections.containsKey(eventListener)) {
            Request request = OkHttpHelper.requestBuilder(oAuthClientService)
                    .url(apiUrl + "/api/homeappliances/" + haId + "/events").header(ACCEPT, TEXT_EVENT_STREAM).build();

            logger.debug("Create new event source listener for '{}'.", haId);
            EventSource eventSource = eventSourceFactory.newEventSource(request,
                    new HomeConnectEventSourceListener(haId, eventListener, this, scheduler, eventQueue));

            eventSourceConnections.put(eventListener, eventSource);
        }
    }

    /**
     * Unregister {@link HomeConnectEventListener}.
     *
     * @param eventListener appliance event listener
     */
    public synchronized void unregisterEventListener(HomeConnectEventListener eventListener) {
        if (eventSourceConnections.containsKey(eventListener)) {
            eventSourceConnections.get(eventListener).cancel();
            eventSourceConnections.remove(eventListener);
        }
    }

    /**
     * Connection count.
     *
     * @return connection count
     */
    public synchronized int connectionSize() {
        return eventSourceConnections.size();
    }

    /**
     * Dispose event source client
     */
    public synchronized void dispose() {
        eventSourceConnections.forEach((key, value) -> value.cancel());
        eventSourceConnections.clear();
    }

    /**
     * Get latest events
     * 
     * @return thread safe queue
     */
    public Queue<Event> getLatestEvents() {
        return eventQueue;
    }

    /**
     * Get latest events by haId
     * 
     * @param haId appliance id
     * @return thread safe queue
     */
    public List<Event> getLatestEvents(String haId) {
        return eventQueue.stream().filter(event -> haId.equals(event.getHaId())).collect(Collectors.toList());
    }
}
