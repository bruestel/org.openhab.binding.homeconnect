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

import static org.eclipse.smarthome.core.library.unit.ImperialUnits.FAHRENHEIT;
import static org.eclipse.smarthome.core.library.unit.SIUnits.CELSIUS;
import static org.eclipse.smarthome.core.library.unit.SmartHomeUnits.PERCENT;
import static org.eclipse.smarthome.core.library.unit.SmartHomeUnits.SECOND;
import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatus.ONLINE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_ACTIVE_PROGRAM_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_BASIC_ACTIONS_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_DOOR_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_DRYER_DRYING_TARGET;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_DURATION;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_ELAPSED_PROGRAM_TIME;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_HOOD_INTENSIVE_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_HOOD_VENTING_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_LOCAL_CONTROL_ACTIVE_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_OPERATION_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_POWER_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_PROGRAM_PROGRESS_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_REMAINING_PROGRAM_TIME_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_REMOTE_CONTROL_ACTIVE_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_REMOTE_START_ALLOWANCE_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_SELECTED_PROGRAM_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_SETPOINT_TEMPERATURE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_WASHER_IDOS1;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_WASHER_IDOS2;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_WASHER_SPIN_SPEED;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_WASHER_TEMPERATURE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_SELECTED;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_START;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_STOP;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_OPERATION_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.HA_ID;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_DRYER_DRYING_TARGET;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_DURATION;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_ELAPSED_PROGRAM_TIME;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_HOOD_INTENSIVE_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_HOOD_VENTING_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_PROGRAM_PROGRESS;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_REMAINING_PROGRAM_TIME;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_SETPOINT_TEMPERATURE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_WASHER_IDOS_1_DOSING_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_WASHER_IDOS_2_DOSING_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_WASHER_SPIN_SPEED;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_WASHER_TEMPERATURE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_01;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_02;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_03;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_04;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_05;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_INTENSIVE_STAGE_1;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_INTENSIVE_STAGE_2;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_INTENSIVE_STAGE_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_DOOR_OPEN;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_EVENT_PRESENT_STATE_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_OPERATION_FINISHED;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_OPERATION_READY;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_OPERATION_RUN;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_POWER_ON;
import static org.openhab.binding.homeconnect.internal.client.model.EventType.CONNECTED;
import static org.openhab.binding.homeconnect.internal.client.model.EventType.DISCONNECTED;
import static org.openhab.binding.homeconnect.internal.client.model.EventType.KEEP_ALIVE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.homeconnect.internal.client.HomeConnectApiClient;
import org.openhab.binding.homeconnect.internal.client.HomeConnectEventSourceClient;
import org.openhab.binding.homeconnect.internal.client.exception.ApplianceOfflineException;
import org.openhab.binding.homeconnect.internal.client.exception.AuthorizationException;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.client.listener.HomeConnectEventListener;
import org.openhab.binding.homeconnect.internal.client.model.AvailableProgramOption;
import org.openhab.binding.homeconnect.internal.client.model.Data;
import org.openhab.binding.homeconnect.internal.client.model.Event;
import org.openhab.binding.homeconnect.internal.client.model.HomeAppliance;
import org.openhab.binding.homeconnect.internal.client.model.Option;
import org.openhab.binding.homeconnect.internal.client.model.Program;
import org.openhab.binding.homeconnect.internal.type.HomeConnectDynamicStateDescriptionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractHomeConnectThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractHomeConnectThingHandler extends BaseThingHandler implements HomeConnectEventListener {

    private static final int CACHE_TTL = 2; // in seconds
    private static final int OFFLINE_MONITOR_1_DELAY = 30; // in min
    private static final int OFFLINE_MONITOR_2_DELAY = 4; // in min
    private static final int EVENT_LISTENER_CONNECT_RETRY_DELAY = 10; // in min

    private @Nullable String operationState;
    private @Nullable ScheduledFuture<?> reinitializationFuture1;
    private @Nullable ScheduledFuture<?> reinitializationFuture2;

    private final ConcurrentHashMap<String, EventHandler> eventHandlers;
    private final ConcurrentHashMap<String, ChannelUpdateHandler> channelUpdateHandlers;
    private final HomeConnectDynamicStateDescriptionProvider dynamicStateDescriptionProvider;
    private final Map<ChannelUID, State> expiringStateMap;
    private final AtomicBoolean accessible;
    private final Logger logger;

    public AbstractHomeConnectThingHandler(Thing thing,
            HomeConnectDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing);
        eventHandlers = new ConcurrentHashMap<>();
        channelUpdateHandlers = new ConcurrentHashMap<>();
        this.dynamicStateDescriptionProvider = dynamicStateDescriptionProvider;
        logger = LoggerFactory.getLogger(AbstractHomeConnectThingHandler.class);
        expiringStateMap = Collections.synchronizedMap(new PassiveExpiringMap<>(TimeUnit.SECONDS.toMillis(CACHE_TTL)));
        accessible = new AtomicBoolean(false);

        configureEventHandlers(eventHandlers);
        configureChannelUpdateHandlers(channelUpdateHandlers);
    }

    @Override
    public void initialize() {
        logger.debug("Initialize thing handler ({}). haId={}", getThingLabel(), getThingHaId());

        if (isBridgeOnline()) {
            refreshThingStatus(); // set ONLINE / OFFLINE
            updateSelectedProgramStateDescription();
            updateChannels();
            registerEventListener();
            scheduleOfflineMonitor1();
            scheduleOfflineMonitor2();
        } else {
            logger.debug("Bridge is offline ({}), skip initialization of thing handler. haId={}", getThingLabel(),
                    getThingHaId());
            updateStatus(OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            accessible.set(false);
        }
    }

    @Override
    public void dispose() {
        unregisterEventListener();
        stopOfflineMonitor1();
        stopOfflineMonitor2();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        logger.debug("Bridge status changed to {} ({}). haId={}", bridgeStatusInfo, getThingLabel(), getThingHaId());

        dispose();
        initialize();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (isThingReadyToHandleCommand()) {
            logger.debug("Handle \"{}\" command ({}). haId={}", command, channelUID.getId(), getThingHaId());
            try {
                Optional<HomeConnectApiClient> homeConnectApiClient = getApiClient();

                if (command instanceof RefreshType) {
                    updateChannel(channelUID);
                } else if (command instanceof StringType && CHANNEL_BASIC_ACTIONS_STATE.equals(channelUID.getId())
                        && homeConnectApiClient.isPresent()) {
                    HomeConnectApiClient apiClient = homeConnectApiClient.get();
                    updateState(channelUID, new StringType(""));

                    if (COMMAND_START.equalsIgnoreCase(command.toFullString())) {
                        @Nullable
                        Bridge bridge = getBridge();
                        if (bridge != null) {
                            @Nullable
                            BridgeHandler bridgeHandler = bridge.getHandler();
                            if (bridgeHandler instanceof HomeConnectBridgeHandler) {
                                HomeConnectBridgeHandler homeConnectBridgeHandler = (HomeConnectBridgeHandler) bridgeHandler;
                                // workaround for api bug
                                // if simulator, program options have to be passed along with the desired program
                                // if non simulator, some options throw a "SDK.Error.UnsupportedOption" error
                                if (homeConnectBridgeHandler.getConfiguration().isSimulator()) {
                                    apiClient.startSelectedProgram(getThingHaId());
                                } else {
                                    @Nullable
                                    Program selectedProgram = apiClient.getSelectedProgram(getThingHaId());
                                    if (selectedProgram != null) {
                                        apiClient.startProgram(getThingHaId(), selectedProgram.getKey());
                                    }
                                }
                            }
                        }

                    } else if (COMMAND_STOP.equalsIgnoreCase(command.toFullString())) {
                        apiClient.stopProgram(getThingHaId());
                    } else if (COMMAND_SELECTED.equalsIgnoreCase(command.toFullString())) {
                        apiClient.getSelectedProgram(getThingHaId());
                    } else {
                        logger.info("Start custom program. command={} haId={}", command.toFullString(), getThingHaId());
                        apiClient.startCustomProgram(getThingHaId(), command.toFullString());
                    }
                } else if (command instanceof StringType && CHANNEL_SELECTED_PROGRAM_STATE.equals(channelUID.getId())
                        && homeConnectApiClient.isPresent()) {
                    homeConnectApiClient.get().setSelectedProgram(getThingHaId(), command.toFullString());
                }
            } catch (ApplianceOfflineException e) {
                logger.debug("Could not handle command {}. Appliance offline. thing={}, haId={}, error={}",
                        command.toFullString(), getThingLabel(), getThingHaId(), e.getMessage());
                updateStatus(OFFLINE);
                resetChannelsOnOfflineEvent();
                resetProgramStateChannels();
            } catch (CommunicationException e) {
                logger.warn("Could not handle command {}. API communication problem! error={}, haId={}",
                        command.toFullString(), e.getMessage(), getThingHaId());
            } catch (AuthorizationException e) {
                logger.warn("Could not handle command {}. Authorization problem! error={}, haId={}",
                        command.toFullString(), e.getMessage(), getThingHaId());

                handleAuthenticationError(e);
            }
        }
    }

    @Override
    public void onEvent(Event event) {
        if (DISCONNECTED.equals(event.getType())) {
            logger.info("Received DISCONNECTED event. Set {} to OFFLINE. haId={}", getThing().getLabel(),
                    getThingHaId());
            updateStatus(OFFLINE);
            resetChannelsOnOfflineEvent();
            resetProgramStateChannels();
        } else if (isThingOnline() && CONNECTED.equals(event.getType())) {
            logger.info("Received CONNECTED event. Update power state channel. haId={}", getThingHaId());
            getThingChannel(CHANNEL_POWER_STATE).ifPresent(c -> updateChannel(c.getUID()));
        } else if (isThingOffline() && !KEEP_ALIVE.equals(event.getType())) {
            updateStatus(ONLINE);
            logger.info("Set {} to ONLINE and update channels. haId={}", getThing().getLabel(), getThingHaId());
            updateChannels();
        }

        if (EVENT_OPERATION_STATE.equals(event.getKey())) {
            operationState = event.getValue() == null ? null : event.getValue();
        }

        if (event.getKey() != null && eventHandlers.containsKey(event.getKey())) {
            eventHandlers.get(event.getKey()).handle(event);
        }

        accessible.set(true);
    }

    @Override
    public void onClosed() {
        unregisterEventListener();
        refreshThingStatus();
        registerEventListener();
    }

    @Override
    public void onRateLimitReached() {
        unregisterEventListener();

        // retry registering
        scheduler.schedule(() -> {
            logger.debug("Try to register event listener again. haId={}", getThingHaId());
            unregisterEventListener();
            registerEventListener();
        }, AbstractHomeConnectThingHandler.EVENT_LISTENER_CONNECT_RETRY_DELAY, TimeUnit.MINUTES);
    }

    /**
     * Register event listener.
     */
    protected void registerEventListener() {
        if (isBridgeOnline() && isThingAccessibleViaServerSentEvents()) {
            getEventSourceClient().ifPresent(client -> {
                try {
                    client.registerEventListener(getThingHaId(), this);
                } catch (CommunicationException | AuthorizationException e) {
                    logger.error("Could not open event source connection. thing={}, haId={}, error={}", getThingLabel(),
                            getThingHaId(), e.getMessage());
                }
            });
        }
    }

    /**
     * Unregister event listener.
     */
    protected void unregisterEventListener() {
        getEventSourceClient().ifPresent(client -> client.unregisterEventListener(this));
    }

    /**
     * Get {@link HomeConnectApiClient}.
     *
     * @return client instance
     */
    protected Optional<HomeConnectApiClient> getApiClient() {
        return getBridgeHandler().map(HomeConnectBridgeHandler::getApiClient);
    }

    /**
     * Get {@link HomeConnectEventSourceClient}.
     *
     * @return client instance if present
     */
    protected Optional<HomeConnectEventSourceClient> getEventSourceClient() {
        return getBridgeHandler().map(HomeConnectBridgeHandler::getEventSourceClient);
    }

    /**
     * Update state description of selected program (Fetch programs via API).
     */
    protected void updateSelectedProgramStateDescription() {
        if (isBridgeOffline() || isThingOffline()) {
            return;
        }

        Optional<HomeConnectApiClient> apiClient = getApiClient();
        if (apiClient.isPresent()) {
            try {
                ArrayList<StateOption> stateOptions = new ArrayList<>();
                apiClient.get().getPrograms(getThingHaId())
                        .forEach(p -> stateOptions.add(new StateOption(p.getKey(), mapStringType(p.getKey()))));

                @Nullable
                StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern("%s")
                        .withReadOnly(stateOptions.isEmpty()).withOptions(stateOptions).build().toStateDescription();

                if (stateDescription != null && !stateOptions.isEmpty()) {
                    getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE).ifPresent(channel -> dynamicStateDescriptionProvider
                            .putStateDescriptions(channel.getUID().getAsString(), stateDescription));
                } else {
                    logger.debug("No state description available. haId={}", getThingHaId());
                    removeSelectedProgramStateDescription();
                }
            } catch (CommunicationException | ApplianceOfflineException | AuthorizationException e) {
                logger.debug("Could not fetch available programs. thing={}, haId={}, error={}", getThingLabel(),
                        getThingHaId(), e.getMessage());
                removeSelectedProgramStateDescription();
            }
        } else {
            removeSelectedProgramStateDescription();
        }
    }

    /**
     * Remove state description of selected program.
     */
    protected void removeSelectedProgramStateDescription() {
        getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE).ifPresent(
                channel -> dynamicStateDescriptionProvider.removeStateDescriptions(channel.getUID().getAsString()));
    }

    /**
     * Is thing ready to process commands. If bridge or thing itself is offline commands will be ignored.
     *
     * @return true if ready
     */
    protected boolean isThingReadyToHandleCommand() {
        @Nullable
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("BridgeHandler not found. Cannot handle command without bridge. thing={}, haId={}",
                    getThingLabel(), getThingHaId());
            return false;
        }

        if (isBridgeOffline()) {
            logger.debug("Bridge is OFFLINE. Ignore command. thing={}, haId={}", getThingLabel(), getThingHaId());
            return false;
        }

        if (isThingOffline()) {
            logger.debug("{} is OFFLINE. Ignore command. haId={}", getThing().getLabel(), getThingHaId());
            return false;
        }

        return true;
    }

    /**
     * Checks if bridge is online and set.
     *
     * @return true if online
     */
    protected boolean isBridgeOnline() {
        @Nullable
        Bridge bridge = getBridge();
        return bridge != null && ONLINE.equals(bridge.getStatus());
    }

    /**
     * Checks if bridge is offline or not set.
     *
     * @return true if offline
     */
    protected boolean isBridgeOffline() {
        return !isBridgeOnline();
    }

    /**
     * Checks if thing is online.
     *
     * @return true if online
     */
    protected boolean isThingOnline() {
        return ONLINE.equals(getThing().getStatus());
    }

    /**
     * Checks if thing is connected to the cloud and accessible via SSE.
     *
     * @return true if yes
     */
    public boolean isThingAccessibleViaServerSentEvents() {
        return accessible.get();
    }

    /**
     * Checks if thing is offline.
     *
     * @return true if offline
     */
    protected boolean isThingOffline() {
        return !isThingOnline();
    }

    /**
     * Get {@link HomeConnectBridgeHandler}.
     *
     * @return bridge handler
     */
    protected Optional<HomeConnectBridgeHandler> getBridgeHandler() {
        @Nullable
        Bridge bridge = getBridge();
        if (bridge != null) {
            @Nullable
            BridgeHandler bridgeHandler = bridge.getHandler();
            if (bridgeHandler instanceof HomeConnectBridgeHandler) {
                return Optional.of((HomeConnectBridgeHandler) bridgeHandler);
            }
        }
        return Optional.empty();
    }

    /**
     * Get thing channel by given channel id.
     *
     * @param channelId channel id
     * @return channel
     */
    protected Optional<Channel> getThingChannel(String channelId) {
        @Nullable
        Channel channel = getThing().getChannel(channelId);
        if (channel == null) {
            return Optional.empty();
        } else {
            return Optional.of(channel);
        }
    }

    /**
     * Configure channel update handlers. Classes which extend {@link AbstractHomeConnectThingHandler} must implement
     * this class and add handlers.
     *
     * @param handlers channel update handlers
     */
    protected abstract void configureChannelUpdateHandlers(
            final ConcurrentHashMap<String, ChannelUpdateHandler> handlers);

    /**
     * Configure event handlers. Classes which extend {@link AbstractHomeConnectThingHandler} must implement
     * this class and add handlers.
     *
     * @param handlers Server-Sent-Event handlers
     */
    protected abstract void configureEventHandlers(final ConcurrentHashMap<String, EventHandler> handlers);

    /**
     * Update all channels via API.
     *
     */
    protected void updateChannels() {
        if (isBridgeOffline()) {
            logger.warn("Bridge handler not found or offline. Stopping update of channels. thing={}, haId={}",
                    getThingLabel(), getThingHaId());
        } else if (isThingOffline()) {
            logger.debug("{} offline. Stopping update of channels. haId={}", getThing().getLabel(), getThingHaId());
        } else {
            List<Channel> channels = getThing().getChannels();
            for (Channel channel : channels) {
                updateChannel(channel.getUID());
            }
        }
    }

    /**
     * Update Channel values via API.
     *
     * @param channelUID channel UID
     */
    protected void updateChannel(ChannelUID channelUID) {
        if (!getApiClient().isPresent()) {
            logger.error("Cannot update channel. No instance of api client found! thing={}, haId={}", getThingLabel(),
                    getThingHaId());
            return;
        }

        if (isBridgeOffline()) {
            logger.warn("BridgeHandler not found or offline. Stopping update of channel {}. thing={}, haId={}",
                    channelUID, getThingLabel(), getThingHaId());
            return;
        }

        if (isThingOffline()) {
            logger.debug("{} offline. Stopping update of channel {}. haId={}", getThing().getLabel(), channelUID,
                    getThingHaId());
            return;
        }

        if (channelUpdateHandlers.containsKey(channelUID.getId())) {
            try {
                channelUpdateHandlers.get(channelUID.getId()).handle(channelUID, expiringStateMap);
            } catch (ApplianceOfflineException e) {
                logger.debug(
                        "API communication problem while trying to update! Appliance offline. thing={}, haId={}, error={}",
                        getThingLabel(), getThingHaId(), e.getMessage());
                updateStatus(OFFLINE);
                resetChannelsOnOfflineEvent();
                resetProgramStateChannels();
            } catch (CommunicationException e) {
                logger.debug("API communication problem while trying to update! thing={}, haId={}, error={}",
                        getThingLabel(), getThingHaId(), e.getMessage());
            } catch (AuthorizationException e) {
                logger.error("Authentication problem while trying to update! thing={}, haId={}", getThingLabel(),
                        getThingHaId(), e);
                handleAuthenticationError(e);
            }
        }
    }

    /**
     * Reset program related channels.
     */
    protected void resetProgramStateChannels() {
        logger.debug("Resetting active program channel states. thing={}, haId={}", getThingLabel(), getThingHaId());
    }

    /**
     * Reset all channels on OFFLINE event.
     */
    protected void resetChannelsOnOfflineEvent() {
        logger.debug("Resetting channel states due to OFFLINE event. thing={}, haId={}", getThingLabel(),
                getThingHaId());
        getThingChannel(CHANNEL_POWER_STATE).ifPresent(channel -> updateState(channel.getUID(), OnOffType.OFF));
        getThingChannel(CHANNEL_OPERATION_STATE).ifPresent(channel -> updateState(channel.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_DOOR_STATE).ifPresent(channel -> updateState(channel.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_LOCAL_CONTROL_ACTIVE_STATE)
                .ifPresent(channel -> updateState(channel.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_REMOTE_CONTROL_ACTIVE_STATE)
                .ifPresent(channel -> updateState(channel.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_REMOTE_START_ALLOWANCE_STATE)
                .ifPresent(channel -> updateState(channel.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE)
                .ifPresent(channel -> updateState(channel.getUID(), UnDefType.NULL));
    }

    /**
     * Map Home Connect key and value names to label.
     * e.g. Dishcare.Dishwasher.Program.Eco50 --> Eco50 or BSH.Common.EnumType.OperationState.DelayedStart --> Delayed
     * Start
     *
     * @param type type
     * @return human readable label
     */
    protected String mapStringType(String type) {
        int index = type.lastIndexOf(".");
        if (index > 0 && type.length() > index) {
            String sub = type.substring(index + 1);
            StringBuilder sb = new StringBuilder();
            for (String word : sub.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
                sb.append(" ");
                sb.append(word);
            }
            return sb.toString().trim();
        }
        return type;
    }

    /**
     * Map Home Connect stage value to label.
     * e.g. Cooking.Hood.EnumType.IntensiveStage.IntensiveStage1 --> 1
     *
     * @param stage stage
     * @return human readable label
     */
    protected String mapStageStringType(String stage) {
        switch (stage) {
            case STAGE_FAN_OFF:
            case STAGE_INTENSIVE_STAGE_OFF:
                stage = "Off";
                break;
            case STAGE_FAN_STAGE_01:
            case STAGE_INTENSIVE_STAGE_1:
                stage = "1";
                break;
            case STAGE_FAN_STAGE_02:
            case STAGE_INTENSIVE_STAGE_2:
                stage = "2";
                break;
            case STAGE_FAN_STAGE_03:
                stage = "3";
                break;
            case STAGE_FAN_STAGE_04:
                stage = "4";
                break;
            case STAGE_FAN_STAGE_05:
                stage = "5";
                break;
            default:
                stage = mapStringType(stage);
        }

        return stage;
    }

    /**
     * Map unit string (returned by home connect api) to Unit
     *
     * @param unit String eg. "°C"
     * @return Unit
     */
    protected Unit<Temperature> mapTemperature(@Nullable String unit) {
        if (unit == null) {
            return CELSIUS;
        } else if (unit.endsWith("C")) {
            return CELSIUS;
        } else {
            return FAHRENHEIT;
        }
    }

    /**
     * Check bridge status and refresh connection status of thing accordingly.
     */
    protected void refreshThingStatus() {
        Optional<HomeConnectApiClient> apiClient = getApiClient();

        apiClient.ifPresent(client -> {
            try {
                HomeAppliance homeAppliance = client.getHomeAppliance(getThingHaId());
                if (!homeAppliance.isConnected()) {
                    logger.debug("Update status to OFFLINE. thing={}, haId={}", getThingLabel(), getThingHaId());
                    updateStatus(OFFLINE);
                } else {
                    logger.debug("Update status to ONLINE. thing={}, haId={}", getThingLabel(), getThingHaId());
                    updateStatus(ONLINE);
                }
                accessible.set(true);
            } catch (CommunicationException | RuntimeException e) {
                logger.debug(
                        "Update status to OFFLINE. Home Connect service is not reachable or a problem occurred!  thing={}, haId={}, error={}.",
                        getThingLabel(), getThingHaId(), e.getMessage());
                updateStatus(OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Home Connect service is not reachable or a problem occurred! (" + e.getMessage() + ").");
                accessible.set(false);
            } catch (AuthorizationException e) {
                logger.debug(
                        "Update status to OFFLINE. Home Connect service is not reachable or a problem occurred!  thing={}, haId={}, error={}",
                        getThingLabel(), getThingHaId(), e.getMessage());
                updateStatus(OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Home Connect service is not reachable or a problem occurred! (" + e.getMessage() + ").");
                accessible.set(false);
                handleAuthenticationError(e);
            }
        });
        if (!apiClient.isPresent()) {
            logger.debug("Update status to OFFLINE (BRIDGE_UNINITIALIZED). thing={}, haId={}", getThingLabel(),
                    getThingHaId());
            updateStatus(OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            accessible.set(false);
        }
    }

    /**
     * Get home appliance id of Thing.
     *
     * @return home appliance id
     */
    public String getThingHaId() {
        return getThing().getConfiguration().get(HA_ID).toString();
    }

    /**
     * Returns the human readable label for this thing.
     *
     * @return the human readable label
     */
    protected @Nullable String getThingLabel() {
        return getThing().getLabel();
    }

    /**
     * Handle authentication exception.
     */
    protected void handleAuthenticationError(AuthorizationException exception) {
        if (isBridgeOnline()) {
            logger.debug(
                    "Thing handler threw authentication exception --> clear credential storage thing={}, haId={} error={}",
                    getThingLabel(), getThingHaId(), exception.getMessage());
            @Nullable
            Bridge bridge = getBridge();
            if (bridge != null) {
                @Nullable
                BridgeHandler bridgeHandler = bridge.getHandler();
                if (bridgeHandler instanceof HomeConnectBridgeHandler) {
                    HomeConnectBridgeHandler homeConnectBridgeHandler = (HomeConnectBridgeHandler) bridgeHandler;

                    try {
                        homeConnectBridgeHandler.getOAuthClientService().remove();
                        homeConnectBridgeHandler.dispose();
                        homeConnectBridgeHandler.initialize();
                    } catch (OAuthException e) {
                        // client is already closed --> we can ignore it
                    }
                }
            }
        }
    }

    /**
     * Get operation state of device.
     *
     * @return operation state string
     */
    protected @Nullable String getOperationState() {
        return operationState;
    }

    protected EventHandler defaultElapsedProgramTimeEventHandler() {
        return event -> getThingChannel(CHANNEL_ELAPSED_PROGRAM_TIME)
                .ifPresent(channel -> updateState(channel.getUID(), new QuantityType<>(event.getValueAsInt(), SECOND)));
    }

    protected EventHandler defaultPowerStateEventHandler() {
        return event -> {
            getThingChannel(CHANNEL_POWER_STATE).ifPresent(channel -> updateState(channel.getUID(),
                    STATE_POWER_ON.equals(event.getValue()) ? OnOffType.ON : OnOffType.OFF));

            if (STATE_POWER_ON.equals(event.getValue())) {
                getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE).ifPresent(c -> updateChannel(c.getUID()));
            } else {
                resetProgramStateChannels();
                getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
            }
        };
    }

    protected EventHandler defaultDoorStateEventHandler() {
        return event -> getThingChannel(CHANNEL_DOOR_STATE).ifPresent(channel -> updateState(channel.getUID(),
                STATE_DOOR_OPEN.equals(event.getValue()) ? OpenClosedType.OPEN : OpenClosedType.CLOSED));
    }

    protected EventHandler defaultOperationStateEventHandler() {
        return event -> {
            @Nullable
            String value = event.getValue();
            getThingChannel(CHANNEL_OPERATION_STATE).ifPresent(channel -> updateState(channel.getUID(),
                    value == null ? UnDefType.NULL : new StringType(mapStringType(value))));

            if (STATE_OPERATION_FINISHED.equals(event.getValue())) {
                getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE)
                        .ifPresent(c -> updateState(c.getUID(), new QuantityType<>(100, PERCENT)));
                getThingChannel(CHANNEL_REMAINING_PROGRAM_TIME_STATE)
                        .ifPresent(c -> updateState(c.getUID(), new QuantityType<>(0, SECOND)));
            } else if (STATE_OPERATION_RUN.equals(event.getValue())) {
                getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE)
                        .ifPresent(c -> updateState(c.getUID(), new QuantityType<>(0, PERCENT)));
                getThingChannel(CHANNEL_ACTIVE_PROGRAM_STATE).ifPresent(c -> updateChannel(c.getUID()));
            } else if (STATE_OPERATION_READY.equals(event.getValue())) {
                resetProgramStateChannels();
            }
        };
    }

    protected EventHandler defaultActiveProgramEventHandler() {
        return event -> {
            @Nullable
            String value = event.getValue();
            getThingChannel(CHANNEL_ACTIVE_PROGRAM_STATE).ifPresent(channel -> updateState(channel.getUID(),
                    value == null ? UnDefType.NULL : new StringType(mapStringType(value))));
            if (event.getValue() == null) {
                resetProgramStateChannels();
            }
        };
    }

    protected EventHandler defaultEventPresentStateEventHandler(String channelId) {
        return event -> getThingChannel(channelId).ifPresent(channel -> updateState(channel.getUID(),
                STATE_EVENT_PRESENT_STATE_OFF.equals(event.getValue()) ? OnOffType.OFF : OnOffType.ON));
    }

    protected EventHandler defaultBooleanEventHandler(String channelId) {
        return event -> getThingChannel(channelId).ifPresent(
                channel -> updateState(channel.getUID(), event.getValueAsBoolean() ? OnOffType.ON : OnOffType.OFF));
    }

    protected EventHandler defaultRemainingProgramTimeEventHandler() {
        return event -> getThingChannel(CHANNEL_REMAINING_PROGRAM_TIME_STATE)
                .ifPresent(channel -> updateState(channel.getUID(), new QuantityType<>(event.getValueAsInt(), SECOND)));
    }

    protected EventHandler defaultSelectedProgramStateEventHandler() {
        return event -> getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE)
                .ifPresent(channel -> updateState(channel.getUID(),
                        event.getValue() == null ? UnDefType.NULL : new StringType(event.getValue())));
    }

    protected EventHandler updateProgramOptionsAndSelectedProgramStateEventHandler() {
        return event -> {
            defaultSelectedProgramStateEventHandler().handle(event);

            // update available program options
            try {
                @Nullable
                String programKey = event.getValue();
                if (programKey != null) {
                    updateProgramOptionsStateDescriptions(programKey);
                }
            } catch (CommunicationException | ApplianceOfflineException | AuthorizationException e) {
                logger.warn("Could not update program options. {}", e.getMessage());
            }
        };
    }

    protected EventHandler defaultProgramProgressEventHandler() {
        return event -> getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE).ifPresent(
                channel -> updateState(channel.getUID(), new QuantityType<>(event.getValueAsInt(), PERCENT)));
    }

    protected ChannelUpdateHandler defaultDoorStateChannelUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                Data data = apiClient.get().getDoorState(getThingHaId());
                if (data.getValue() != null) {
                    return STATE_DOOR_OPEN.equals(data.getValue()) ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
                } else {
                    return UnDefType.NULL;
                }
            } else {
                return UnDefType.NULL;
            }
        }));
    }

    protected ChannelUpdateHandler defaultPowerStateChannelUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                Data data = apiClient.get().getPowerState(getThingHaId());
                if (data.getValue() != null) {
                    return STATE_POWER_ON.equals(data.getValue()) ? OnOffType.ON : OnOffType.OFF;
                } else {
                    return UnDefType.NULL;
                }
            } else {
                return UnDefType.NULL;
            }
        }));
    }

    protected ChannelUpdateHandler defaultNoOpUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, UnDefType.NULL);
    }

    protected ChannelUpdateHandler defaultOperationStateChannelUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                Data data = apiClient.get().getOperationState(getThingHaId());

                @Nullable
                String value = data.getValue();
                if (value != null) {
                    operationState = data.getValue();
                    return new StringType(mapStringType(value));
                } else {
                    operationState = null;
                    return UnDefType.NULL;
                }
            } else {
                return UnDefType.NULL;
            }
        }));
    }

    protected ChannelUpdateHandler defaultRemoteControlActiveStateChannelUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                return apiClient.get().isRemoteControlActive(getThingHaId()) ? OnOffType.ON : OnOffType.OFF;
            }
            return OnOffType.OFF;
        }));
    }

    protected ChannelUpdateHandler defaultLocalControlActiveStateChannelUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                return apiClient.get().isLocalControlActive(getThingHaId()) ? OnOffType.ON : OnOffType.OFF;
            }
            return OnOffType.OFF;
        }));
    }

    protected ChannelUpdateHandler defaultRemoteStartAllowanceChannelUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                return apiClient.get().isRemoteControlStartAllowed(getThingHaId()) ? OnOffType.ON : OnOffType.OFF;
            }
            return OnOffType.OFF;
        }));
    }

    protected ChannelUpdateHandler defaultSelectedProgramStateUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                @Nullable
                Program program = apiClient.get().getSelectedProgram(getThingHaId());
                if (program != null) {
                    processProgramOptions(program.getOptions());
                    return new StringType(program.getKey());
                } else {
                    return UnDefType.NULL;
                }
            }
            return UnDefType.NULL;
        }));
    }

    protected ChannelUpdateHandler updateProgramOptionsStateDescriptionsAndSelectedProgramStateUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                @Nullable
                Program program = apiClient.get().getSelectedProgram(getThingHaId());

                if (program != null) {
                    updateProgramOptionsStateDescriptions(program.getKey());
                    processProgramOptions(program.getOptions());

                    return new StringType(program.getKey());
                } else {
                    return UnDefType.NULL;
                }
            }
            return UnDefType.NULL;
        }));
    }

    protected ChannelUpdateHandler defaultActiveProgramStateUpdateHandler() {
        return (channelUID, cache) -> updateState(channelUID, cachePutIfAbsentAndGet(channelUID, cache, () -> {
            Optional<HomeConnectApiClient> apiClient = getApiClient();
            if (apiClient.isPresent()) {
                @Nullable
                Program program = apiClient.get().getActiveProgram(getThingHaId());

                if (program != null) {
                    processProgramOptions(program.getOptions());
                    return new StringType(mapStringType(program.getKey()));
                } else {
                    resetProgramStateChannels();
                    return UnDefType.NULL;
                }
            }
            return UnDefType.NULL;
        }));
    }

    protected void processProgramOptions(List<Option> options) {
        options.forEach(option -> {
            @Nullable
            String key = option.getKey();
            if (key != null) {
                switch (key) {
                    case OPTION_WASHER_TEMPERATURE:
                        getThingChannel(CHANNEL_WASHER_TEMPERATURE)
                                .ifPresent(channel -> updateState(channel.getUID(), new StringType(option.getValue())));
                        break;
                    case OPTION_WASHER_SPIN_SPEED:
                        getThingChannel(CHANNEL_WASHER_SPIN_SPEED)
                                .ifPresent(channel -> updateState(channel.getUID(), new StringType(option.getValue())));
                        break;
                    case OPTION_WASHER_IDOS_1_DOSING_LEVEL:
                        getThingChannel(CHANNEL_WASHER_IDOS1)
                                .ifPresent(channel -> updateState(channel.getUID(), new StringType(option.getValue())));
                        break;
                    case OPTION_WASHER_IDOS_2_DOSING_LEVEL:
                        getThingChannel(CHANNEL_WASHER_IDOS2)
                                .ifPresent(channel -> updateState(channel.getUID(), new StringType(option.getValue())));
                        break;
                    case OPTION_DRYER_DRYING_TARGET:
                        getThingChannel(CHANNEL_DRYER_DRYING_TARGET)
                                .ifPresent(channel -> updateState(channel.getUID(), new StringType(option.getValue())));
                        break;
                    case OPTION_HOOD_INTENSIVE_LEVEL:
                        @Nullable
                        String hoodIntensiveLevelValue = option.getValue();
                        if (hoodIntensiveLevelValue != null) {
                            getThingChannel(CHANNEL_HOOD_INTENSIVE_LEVEL)
                                    .ifPresent(channel -> updateState(channel.getUID(),
                                            new StringType(mapStageStringType(hoodIntensiveLevelValue))));
                        }
                        break;
                    case OPTION_HOOD_VENTING_LEVEL:
                        @Nullable
                        String hoodVentingLevel = option.getValue();
                        if (hoodVentingLevel != null) {
                            getThingChannel(CHANNEL_HOOD_VENTING_LEVEL)
                                    .ifPresent(channel -> updateState(channel.getUID(),
                                            new StringType(mapStageStringType(hoodVentingLevel))));
                        }
                        break;
                    case OPTION_SETPOINT_TEMPERATURE:
                        getThingChannel(CHANNEL_SETPOINT_TEMPERATURE).ifPresent(channel -> updateState(channel.getUID(),
                                new QuantityType<>(option.getValueAsInt(), mapTemperature(option.getUnit()))));
                        break;
                    case OPTION_DURATION:
                        getThingChannel(CHANNEL_DURATION).ifPresent(channel -> updateState(channel.getUID(),
                                new QuantityType<>(option.getValueAsInt(), SECOND)));
                        break;
                    case OPTION_REMAINING_PROGRAM_TIME:
                        getThingChannel(CHANNEL_REMAINING_PROGRAM_TIME_STATE)
                                .ifPresent(channel -> updateState(channel.getUID(),
                                        new QuantityType<>(option.getValueAsInt(), SECOND)));
                        break;
                    case OPTION_ELAPSED_PROGRAM_TIME:
                        getThingChannel(CHANNEL_ELAPSED_PROGRAM_TIME).ifPresent(channel -> updateState(channel.getUID(),
                                new QuantityType<>(option.getValueAsInt(), SECOND)));
                        break;
                    case OPTION_PROGRAM_PROGRESS:
                        getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE)
                                .ifPresent(channel -> updateState(channel.getUID(),
                                        new QuantityType<>(option.getValueAsInt(), PERCENT)));
                        break;
                }
            }
        });
    }

    protected State cachePutIfAbsentAndGet(ChannelUID channelUID, Map<ChannelUID, State> cache,
            SupplierWithException<State> supplier)
            throws AuthorizationException, ApplianceOfflineException, CommunicationException {

        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (cache) {
            State state = cache.get(channelUID);
            if (state == null) {
                state = supplier.get();
                cache.put(channelUID, state);
            }
            return state;
        }
    }

    protected String convertWasherTemperature(String value) {
        if (value.startsWith("LaundryCare.Washer.EnumType.Temperature.GC")) {
            return value.replace("LaundryCare.Washer.EnumType.Temperature.GC", "") + "°C";
        }

        if (value.startsWith("LaundryCare.Washer.EnumType.Temperature.Ul")) {
            return mapStringType(value.replace("LaundryCare.Washer.EnumType.Temperature.Ul", ""));
        }

        return mapStringType(value);
    }

    protected String convertWasherSpinSpeed(String value) {
        if (value.startsWith("LaundryCare.Washer.EnumType.SpinSpeed.RPM")) {
            return value.replace("LaundryCare.Washer.EnumType.SpinSpeed.RPM", "") + " RPM";
        }

        if (value.startsWith("LaundryCare.Washer.EnumType.SpinSpeed.Ul")) {
            return value.replace("LaundryCare.Washer.EnumType.SpinSpeed.Ul", "");
        }

        return mapStringType(value);
    }

    protected void updateProgramOptionsStateDescriptions(String programKey)
            throws CommunicationException, AuthorizationException, ApplianceOfflineException {
        Optional<HomeConnectApiClient> apiClient = getApiClient();
        if (apiClient.isPresent()) {
            List<AvailableProgramOption> availableProgramOptions = apiClient.get().getProgramOptions(getThingHaId(),
                    programKey);

            Optional<Channel> channelSpinSpeed = getThingChannel(CHANNEL_WASHER_SPIN_SPEED);
            Optional<Channel> channelTemperature = getThingChannel(CHANNEL_WASHER_TEMPERATURE);
            Optional<Channel> channelDryingTarget = getThingChannel(CHANNEL_DRYER_DRYING_TARGET);

            if (availableProgramOptions.isEmpty()) {
                channelSpinSpeed.ifPresent(channel -> dynamicStateDescriptionProvider
                        .removeStateDescriptions(channel.getUID().getAsString()));
                channelTemperature.ifPresent(channel -> dynamicStateDescriptionProvider
                        .removeStateDescriptions(channel.getUID().getAsString()));
                channelDryingTarget.ifPresent(channel -> dynamicStateDescriptionProvider
                        .removeStateDescriptions(channel.getUID().getAsString()));
            }

            availableProgramOptions.forEach(option -> {
                switch (option.getKey()) {
                    case OPTION_WASHER_SPIN_SPEED: {
                        createStateDescription(option, this::convertWasherSpinSpeed)
                                .ifPresent(stateDescription -> channelSpinSpeed
                                        .ifPresent(channel -> dynamicStateDescriptionProvider.putStateDescriptions(
                                                channel.getUID().getAsString(), stateDescription)));
                        break;
                    }
                    case OPTION_WASHER_TEMPERATURE: {
                        createStateDescription(option, this::convertWasherTemperature)
                                .ifPresent(stateDescription -> channelTemperature
                                        .ifPresent(channel -> dynamicStateDescriptionProvider.putStateDescriptions(
                                                channel.getUID().getAsString(), stateDescription)));
                        break;
                    }
                    case OPTION_DRYER_DRYING_TARGET: {
                        createStateDescription(option, this::mapStringType)
                                .ifPresent(stateDescription -> channelDryingTarget
                                        .ifPresent(channel -> dynamicStateDescriptionProvider.putStateDescriptions(
                                                channel.getUID().getAsString(), stateDescription)));
                        break;
                    }
                }
            });
        }
    }

    protected HomeConnectDynamicStateDescriptionProvider getDynamicStateDescriptionProvider() {
        return dynamicStateDescriptionProvider;
    }

    private Optional<StateDescription> createStateDescription(AvailableProgramOption option,
            Function<String, String> stateConverter) {
        ArrayList<StateOption> stateOptions = new ArrayList<>();
        option.getAllowedValues().forEach(av -> stateOptions.add(new StateOption(av, stateConverter.apply(av))));

        @Nullable
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern("%s")
                .withReadOnly(stateOptions.isEmpty()).withOptions(stateOptions).build().toStateDescription();

        return stateDescription == null ? Optional.empty() : Optional.of(stateDescription);
    }

    private synchronized void scheduleOfflineMonitor1() {
        this.reinitializationFuture1 = scheduler.schedule(() -> {
            if (isBridgeOnline() && isThingOffline()) {
                logger.debug("Offline monitor 1: Check if thing is ONLINE. thing={}, haId={}", getThingLabel(),
                        getThingHaId());
                refreshThingStatus();
                if (isThingOnline()) {
                    logger.debug("Offline monitor 1: Thing status changed to ONLINE. thing={}, haId={}",
                            getThingLabel(), getThingHaId());
                    dispose();
                    initialize();
                } else {
                    scheduleOfflineMonitor1();
                }
            } else {
                scheduleOfflineMonitor1();
            }
        }, AbstractHomeConnectThingHandler.OFFLINE_MONITOR_1_DELAY, TimeUnit.MINUTES);
    }

    private synchronized void stopOfflineMonitor1() {
        @Nullable
        ScheduledFuture<?> reinitializationFuture = this.reinitializationFuture1;
        if (reinitializationFuture != null) {
            reinitializationFuture.cancel(false);
        }
    }

    private synchronized void scheduleOfflineMonitor2() {
        this.reinitializationFuture2 = scheduler.schedule(() -> {
            if (isBridgeOnline() && !accessible.get()) {
                logger.debug("Offline monitor 2: Check if thing is ONLINE. thing={}, haId={}", getThingLabel(),
                        getThingHaId());
                refreshThingStatus();
                if (isThingOnline()) {
                    logger.debug("Offline monitor 2: Thing status changed to ONLINE. thing={}, haId={}",
                            getThingLabel(), getThingHaId());
                    dispose();
                    initialize();
                } else {
                    scheduleOfflineMonitor2();
                }
            } else {
                scheduleOfflineMonitor2();
            }
        }, AbstractHomeConnectThingHandler.OFFLINE_MONITOR_2_DELAY, TimeUnit.MINUTES);
    }

    private synchronized void stopOfflineMonitor2() {
        @Nullable
        ScheduledFuture<?> reinitializationFuture = this.reinitializationFuture2;
        if (reinitializationFuture != null) {
            reinitializationFuture.cancel(false);
        }
    }
}
