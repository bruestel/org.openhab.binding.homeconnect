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
package org.openhab.binding.homeconnect.internal.handler;

import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.API_BASE_URL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.API_SIMULATOR_BASE_URL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OAUTH_AUTHORIZE_PATH;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OAUTH_SCOPE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OAUTH_TOKEN_PATH;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenResponse;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthFactory;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthResponseException;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.homeconnect.internal.client.HomeConnectApiClient;
import org.openhab.binding.homeconnect.internal.client.HomeConnectEventSourceClient;
import org.openhab.binding.homeconnect.internal.client.exception.AuthorizationException;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.client.model.ApiRequest;
import org.openhab.binding.homeconnect.internal.client.model.Event;
import org.openhab.binding.homeconnect.internal.configuration.ApiBridgeConfiguration;
import org.openhab.binding.homeconnect.internal.servlet.HomeConnectServlet;
import org.openhab.core.OpenHAB;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeConnectBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public class HomeConnectBridgeHandler extends BaseBridgeHandler {

    private static final int REINITIALIZATION_DELAY = 120;
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String CLIENT_ID = "clientId";

    private final OAuthFactory oAuthFactory;
    private final HomeConnectServlet homeConnectServlet;
    private final Logger logger;

    private @Nullable ScheduledFuture<?> reinitializationFuture;
    private @Nullable List<ApiRequest> apiRequestHistory;
    private @Nullable List<Event> eventHistory;

    private @NonNullByDefault({}) OAuthClientService oAuthClientService;
    private @NonNullByDefault({}) String oAuthServiceHandleId;
    private @NonNullByDefault({}) HomeConnectApiClient apiClient;
    private @NonNullByDefault({}) HomeConnectEventSourceClient eventSourceClient;

    public HomeConnectBridgeHandler(Bridge bridge, OAuthFactory oAuthFactory, HomeConnectServlet homeConnectServlet) {
        super(bridge);

        this.oAuthFactory = oAuthFactory;
        this.homeConnectServlet = homeConnectServlet;
        logger = LoggerFactory.getLogger(HomeConnectBridgeHandler.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // not used for bridge
    }

    @Override
    public void initialize() {
        Version version = FrameworkUtil.getBundle(this.getClass()).getVersion();
        String openHabVersion = OpenHAB.getVersion();
        logger.debug("Initialize bridge {} (Bundle: {}, openHAB: {})", getThing().getLabel(), version.toString(),
                openHabVersion);
        // let the bridge configuration servlet know about this handler
        homeConnectServlet.addBridgeHandler(this);

        // create oAuth service
        ApiBridgeConfiguration config = getConfiguration();
        String tokenUrl = (config.isSimulator() ? API_SIMULATOR_BASE_URL : API_BASE_URL) + OAUTH_TOKEN_PATH;
        String authorizeUrl = (config.isSimulator() ? API_SIMULATOR_BASE_URL : API_BASE_URL) + OAUTH_AUTHORIZE_PATH;
        String oAuthServiceHandleId = thing.getUID().getAsString() + (config.isSimulator() ? "simulator" : "");

        oAuthClientService = oAuthFactory.createOAuthClientService(oAuthServiceHandleId, tokenUrl, authorizeUrl,
                config.getClientId(), config.getClientSecret(), OAUTH_SCOPE, true);
        this.oAuthServiceHandleId = oAuthServiceHandleId;
        logger.debug(
                "Initialize oAuth client service. tokenUrl={}, authorizeUrl={}, oAuthServiceHandleId={}, scope={}, oAuthClientService={}",
                tokenUrl, authorizeUrl, oAuthServiceHandleId, OAUTH_SCOPE, oAuthClientService);

        // create api client
        apiClient = new HomeConnectApiClient(oAuthClientService, config.isSimulator(), apiRequestHistory);
        eventSourceClient = new HomeConnectEventSourceClient(oAuthClientService, config.isSimulator(), scheduler,
                eventHistory);

        try {
            @Nullable
            AccessTokenResponse accessTokenResponse = oAuthClientService.getAccessTokenResponse();

            if (accessTokenResponse == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                        "Please authenticate your account at http(s)://[YOUROPENHAB]:[YOURPORT]/homeconnect (e.g. http://192.168.178.100:8080/homeconnect).");
                logger.info(
                        "Configuration is pending. Please authenticate your account at http(s)://[YOUROPENHAB]:[YOURPORT]/homeconnect (e.g. http://192.168.178.100:8080/homeconnect). bridge={}",
                        getThing().getLabel());
            } else {
                apiClient.getHomeAppliances();
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (OAuthException | IOException | OAuthResponseException | CommunicationException
                | AuthorizationException e) {
            ZonedDateTime nextReinitializeDateTime = ZonedDateTime.now().plusSeconds(REINITIALIZATION_DELAY);

            String infoMessage = String.format(
                    "Home Connect service is not reachable or a problem occurred! Retrying at %s (%s). bridge=%s",
                    nextReinitializeDateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME), e.getMessage(),
                    getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, infoMessage);
            logger.info(infoMessage);

            scheduleReinitialize();
        }
    }

    @Override
    public void dispose() {
        logger.debug("Dispose bridge {}", getThing().getLabel());
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.DUTY_CYCLE);
        stopReinitializer();
        cleanup();
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        if (isModifyingCurrentConfig(configurationParameters)) {
            List<String> parameters = configurationParameters.entrySet().stream().map((entry) -> {
                if (CLIENT_ID.equals(entry.getKey()) || CLIENT_SECRET.equals(entry.getKey())) {
                    return entry.getKey() + ": ***";
                }
                return entry.getKey() + ": " + entry.getValue();
            }).collect(Collectors.toList());

            logger.info("Update bridge configuration. bridge={}, parameters={}", getThing().getLabel(), parameters);

            validateConfigurationParameters(configurationParameters);
            Configuration configuration = editConfiguration();
            for (Entry<String, Object> configurationParameter : configurationParameters.entrySet()) {
                configuration.put(configurationParameter.getKey(), configurationParameter.getValue());
            }

            // invalidate oAuth credentials
            try {
                logger.info("Clear oAuth credential store. bridge={}", getThing().getLabel());
                oAuthClientService.remove();
            } catch (OAuthException e) {
                logger.error("Could not clear oAuth credentials. bridge={}", getThing().getLabel(), e);
            }

            if (isInitialized()) {
                // persist new configuration and reinitialize handler
                dispose();
                updateConfiguration(configuration);
                initialize();
            } else {
                // persist new configuration and notify Thing Manager
                updateConfiguration(configuration);
                @Nullable
                ThingHandlerCallback callback = getCallback();
                if (callback != null) {
                    callback.configurationUpdated(this.getThing());
                } else {
                    logger.warn(
                            "Handler {} tried updating its configuration although the handler was already disposed.",
                            this.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Get {@link HomeConnectApiClient}.
     *
     * @return api client instance
     */
    public HomeConnectApiClient getApiClient() {
        return apiClient;
    }

    /**
     * Get {@link HomeConnectEventSourceClient}.
     *
     * @return event source client instance
     */
    public HomeConnectEventSourceClient getEventSourceClient() {
        return eventSourceClient;
    }

    /**
     * Get children of bridge
     *
     * @return list of child handlers
     */
    public List<AbstractHomeConnectThingHandler> getThingHandler() {
        return getThing().getThings().stream()
                .filter(thing -> thing.getHandler() instanceof AbstractHomeConnectThingHandler)
                .map(thing -> (AbstractHomeConnectThingHandler) thing.getHandler()).collect(Collectors.toList());
    }

    /**
     * Get {@link ApiBridgeConfiguration}.
     *
     * @return bridge configuration (clientId, clientSecret, etc.)
     */
    public ApiBridgeConfiguration getConfiguration() {
        return getConfigAs(ApiBridgeConfiguration.class);
    }

    /**
     * Get {@link OAuthClientService} instance.
     *
     * @return oAuth client service instance
     */
    public OAuthClientService getOAuthClientService() {
        return oAuthClientService;
    }

    private void cleanup() {
        apiRequestHistory = new ArrayList<>();
        apiRequestHistory.addAll(apiClient.getLatestApiRequests());
        apiClient.getLatestApiRequests().clear();

        eventHistory = new ArrayList<>();
        eventHistory.addAll(eventSourceClient.getLatestEvents());
        eventSourceClient.getLatestEvents().clear();
        eventSourceClient.dispose();

        oAuthFactory.ungetOAuthService(oAuthServiceHandleId);
        homeConnectServlet.removeBridgeHandler(this);
    }

    private synchronized void scheduleReinitialize() {
        @Nullable
        ScheduledFuture<?> reinitializationFuture = this.reinitializationFuture;
        if (reinitializationFuture != null && !reinitializationFuture.isDone()) {
            logger.debug("Reinitialization is already scheduled. Starting in {} seconds. bridge={}",
                    reinitializationFuture.getDelay(TimeUnit.SECONDS), getThing().getLabel());
        } else {
            this.reinitializationFuture = scheduler.schedule(() -> {
                cleanup();
                initialize();
            }, HomeConnectBridgeHandler.REINITIALIZATION_DELAY, TimeUnit.SECONDS);
        }
    }

    private synchronized void stopReinitializer() {
        @Nullable
        ScheduledFuture<?> reinitializationFuture = this.reinitializationFuture;
        if (reinitializationFuture != null) {
            reinitializationFuture.cancel(true);
        }
    }
}
