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

import static java.lang.String.format;
import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_ACTIVE_PROGRAM_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_HOOD_ACTIONS_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_HOOD_INTENSIVE_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_HOOD_VENTING_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_LOCAL_CONTROL_ACTIVE_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_OPERATION_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_POWER_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_REMOTE_CONTROL_ACTIVE_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.CHANNEL_REMOTE_START_ALLOWANCE_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_AUTOMATIC;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_DELAYED_SHUT_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_STOP;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_VENTING_1;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_VENTING_2;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_VENTING_3;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_VENTING_4;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_VENTING_5;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_VENTING_INTENSIVE_1;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.COMMAND_VENTING_INTENSIVE_2;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_ACTIVE_PROGRAM;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_HOOD_INTENSIVE_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_HOOD_VENTING_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_LOCAL_CONTROL_ACTIVE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_OPERATION_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_POWER_STATE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_REMOTE_CONTROL_ACTIVE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.EVENT_REMOTE_CONTROL_START_ALLOWED;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPERATION_STATE_INACTIVE;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPERATION_STATE_RUN;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_HOOD_INTENSIVE_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.OPTION_HOOD_VENTING_LEVEL;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.PROGRAM_HOOD_AUTOMATIC;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.PROGRAM_HOOD_DELAYED_SHUT_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.PROGRAM_HOOD_VENTING;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_01;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_02;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_03;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_04;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_FAN_STAGE_05;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_INTENSIVE_STAGE_1;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_INTENSIVE_STAGE_2;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STAGE_INTENSIVE_STAGE_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_POWER_OFF;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.STATE_POWER_ON;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.homeconnect.internal.client.HomeConnectApiClient;
import org.openhab.binding.homeconnect.internal.client.exception.ApplianceOfflineException;
import org.openhab.binding.homeconnect.internal.client.exception.AuthorizationException;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.type.HomeConnectDynamicStateDescriptionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeConnectHoodHandler} is responsible for handling commands, which are
 * sent to one of the channels of a hood.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public class HomeConnectHoodHandler extends AbstractHomeConnectThingHandler {

    private final static String START_VENTING_INTENSIVE_STAGE_PAYLOAD_TEMPLATE = "\n" + "{\n" + "    \"data\": {\n"
            + "        \"key\": \"Cooking.Common.Program.Hood.Venting\",\n" + "        \"options\": [\n"
            + "            {\n" + "                \"key\": \"Cooking.Common.Option.Hood.IntensiveLevel\",\n"
            + "                \"value\": \"%s\"\n" + "            }\n" + "        ]\n" + "    }\n" + "}";

    private final static String START_VENTING_STAGE_PAYLOAD_TEMPLATE = "\n" + "{\n" + "    \"data\": {\n"
            + "        \"key\": \"Cooking.Common.Program.Hood.Venting\",\n" + "        \"options\": [\n"
            + "            {\n" + "                \"key\": \"Cooking.Common.Option.Hood.VentingLevel\",\n"
            + "                \"value\": \"%s\"\n" + "            }\n" + "        ]\n" + "    }\n" + "}";

    private final Logger logger;

    public HomeConnectHoodHandler(Thing thing,
            HomeConnectDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing, dynamicStateDescriptionProvider);
        logger = LoggerFactory.getLogger(HomeConnectHoodHandler.class);
        resetProgramStateChannels();
    }

    @Override
    protected void configureChannelUpdateHandlers(ConcurrentHashMap<String, ChannelUpdateHandler> handlers) {
        // register default update handlers
        handlers.put(CHANNEL_OPERATION_STATE, defaultOperationStateChannelUpdateHandler());
        handlers.put(CHANNEL_POWER_STATE, defaultPowerStateChannelUpdateHandler());
        handlers.put(CHANNEL_REMOTE_START_ALLOWANCE_STATE, defaultRemoteStartAllowanceChannelUpdateHandler());
        handlers.put(CHANNEL_REMOTE_CONTROL_ACTIVE_STATE, defaultRemoteControlActiveStateChannelUpdateHandler());
        handlers.put(CHANNEL_LOCAL_CONTROL_ACTIVE_STATE, defaultLocalControlActiveStateChannelUpdateHandler());
        handlers.put(CHANNEL_ACTIVE_PROGRAM_STATE, defaultActiveProgramStateUpdateHandler());
    }

    @Override
    protected void configureEventHandlers(ConcurrentHashMap<String, EventHandler> handlers) {
        // register default SSE event handlers
        handlers.put(EVENT_REMOTE_CONTROL_START_ALLOWED,
                defaultBooleanEventHandler(CHANNEL_REMOTE_START_ALLOWANCE_STATE));
        handlers.put(EVENT_REMOTE_CONTROL_ACTIVE, defaultBooleanEventHandler(CHANNEL_REMOTE_CONTROL_ACTIVE_STATE));
        handlers.put(EVENT_LOCAL_CONTROL_ACTIVE, defaultBooleanEventHandler(CHANNEL_LOCAL_CONTROL_ACTIVE_STATE));
        handlers.put(EVENT_OPERATION_STATE, defaultOperationStateEventHandler());
        handlers.put(EVENT_ACTIVE_PROGRAM, defaultActiveProgramEventHandler());
        handlers.put(EVENT_POWER_STATE, defaultPowerStateEventHandler());

        // register hood specific SSE event handlers
        handlers.put(EVENT_HOOD_INTENSIVE_LEVEL,
                event -> getThingChannel(CHANNEL_HOOD_INTENSIVE_LEVEL).ifPresent(channel -> {
                    @Nullable
                    String hoodIntensiveLevel = event.getValue();
                    if (hoodIntensiveLevel != null) {
                        updateState(channel.getUID(), new StringType(mapStageStringType(hoodIntensiveLevel)));
                    } else {
                        updateState(channel.getUID(), UnDefType.NULL);
                    }
                }));
        handlers.put(EVENT_HOOD_VENTING_LEVEL,
                event -> getThingChannel(CHANNEL_HOOD_VENTING_LEVEL).ifPresent(channel -> {
                    @Nullable
                    String hoodVentingLevel = event.getValue();
                    if (hoodVentingLevel != null) {
                        updateState(channel.getUID(), new StringType(mapStageStringType(hoodVentingLevel)));
                    } else {
                        updateState(channel.getUID(), UnDefType.NULL);
                    }
                }));
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (isBridgeOnline() && isThingAccessibleViaServerSentEvents()) {
            super.handleCommand(channelUID, command);

            getApiClient().ifPresent(apiClient -> {
                try {
                    // turn hood on and off
                    if (command instanceof OnOffType && CHANNEL_POWER_STATE.equals(channelUID.getId())) {
                        apiClient.setPowerState(getThingHaId(),
                                OnOffType.ON.equals(command) ? STATE_POWER_ON : STATE_POWER_OFF);
                    }

                    // program options
                    if (command instanceof StringType && CHANNEL_HOOD_ACTIONS_STATE.equals(channelUID.getId())) {
                        @Nullable
                        String operationState = getOperationState();
                        if (OPERATION_STATE_INACTIVE.equals(operationState)
                                || OPERATION_STATE_RUN.equals(operationState)) {
                            if (COMMAND_STOP.equalsIgnoreCase(command.toFullString())) {
                                apiClient.stopProgram(getThingHaId());
                            }
                        } else {
                            logger.debug(
                                    "Device can not handle command {} in current operation state ({}). thing={}, haId={}",
                                    command, operationState, getThingLabel(), getThingHaId());
                        }

                        // These command always start the hood - even if appliance is turned off
                        if (COMMAND_AUTOMATIC.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startProgram(getThingHaId(), PROGRAM_HOOD_AUTOMATIC);
                        } else if (COMMAND_DELAYED_SHUT_OFF.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startProgram(getThingHaId(), PROGRAM_HOOD_DELAYED_SHUT_OFF);
                        } else if (COMMAND_VENTING_1.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startCustomProgram(getThingHaId(),
                                    format(START_VENTING_STAGE_PAYLOAD_TEMPLATE, STAGE_FAN_STAGE_01));
                        } else if (COMMAND_VENTING_2.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startCustomProgram(getThingHaId(),
                                    format(START_VENTING_STAGE_PAYLOAD_TEMPLATE, STAGE_FAN_STAGE_02));
                        } else if (COMMAND_VENTING_3.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startCustomProgram(getThingHaId(),
                                    format(START_VENTING_STAGE_PAYLOAD_TEMPLATE, STAGE_FAN_STAGE_03));
                        } else if (COMMAND_VENTING_4.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startCustomProgram(getThingHaId(),
                                    format(START_VENTING_STAGE_PAYLOAD_TEMPLATE, STAGE_FAN_STAGE_04));
                        } else if (COMMAND_VENTING_5.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startCustomProgram(getThingHaId(),
                                    format(START_VENTING_STAGE_PAYLOAD_TEMPLATE, STAGE_FAN_STAGE_05));
                        } else if (COMMAND_VENTING_INTENSIVE_1.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startCustomProgram(getThingHaId(),
                                    format(START_VENTING_INTENSIVE_STAGE_PAYLOAD_TEMPLATE, STAGE_INTENSIVE_STAGE_1));
                        } else if (COMMAND_VENTING_INTENSIVE_2.equalsIgnoreCase(command.toFullString())) {
                            apiClient.startCustomProgram(getThingHaId(),
                                    format(START_VENTING_INTENSIVE_STAGE_PAYLOAD_TEMPLATE, STAGE_INTENSIVE_STAGE_2));
                        } else {
                            logger.info("Start custom program. command={} haId={}", command.toFullString(),
                                    getThingHaId());
                            apiClient.startCustomProgram(getThingHaId(), command.toFullString());
                        }
                    }
                } catch (ApplianceOfflineException e) {
                    logger.debug("Could not handle command {}. Appliance offline. thing={}, haId={}, error={}",
                            command.toFullString(), getThingLabel(), getThingHaId(), e.getMessage());
                    updateStatus(OFFLINE);
                    resetChannelsOnOfflineEvent();
                    resetProgramStateChannels();
                } catch (CommunicationException e) {
                    logger.warn("Could not handle command {}. API communication problem! thing={}, haId={}, error={}",
                            command.toFullString(), getThingLabel(), getThingHaId(), e.getMessage());
                } catch (AuthorizationException e) {
                    logger.warn("Could not handle command {}. Authorization problem! thing={}, haId={}, error={}",
                            command.toFullString(), getThingLabel(), getThingHaId(), e.getMessage());

                    handleAuthenticationError(e);
                }
            });
        }
    }

    @Override
    protected void updateSelectedProgramStateDescription() {
        // update hood program actions
        if (isBridgeOffline() || !isThingAccessibleViaServerSentEvents()) {
            return;
        }

        Optional<HomeConnectApiClient> apiClient = getApiClient();
        if (apiClient.isPresent()) {
            try {
                ArrayList<StateOption> stateOptions = new ArrayList<>();
                apiClient.get().getPrograms(getThingHaId()).forEach(availableProgram -> {
                    if (PROGRAM_HOOD_AUTOMATIC.equals(availableProgram.getKey())) {
                        stateOptions.add(new StateOption(COMMAND_AUTOMATIC, mapStringType(availableProgram.getKey())));
                    } else if (PROGRAM_HOOD_DELAYED_SHUT_OFF.equals(availableProgram.getKey())) {
                        stateOptions.add(
                                new StateOption(COMMAND_DELAYED_SHUT_OFF, mapStringType(availableProgram.getKey())));
                    } else if (PROGRAM_HOOD_VENTING.equals(availableProgram.getKey())) {
                        try {
                            apiClient.get().getProgramOptions(getThingHaId(), PROGRAM_HOOD_VENTING).forEach(option -> {
                                if (OPTION_HOOD_VENTING_LEVEL.equalsIgnoreCase(option.getKey())) {
                                    option.getAllowedValues().stream().filter(s -> !STAGE_FAN_OFF.equalsIgnoreCase(s))
                                            .forEach(s -> stateOptions.add(createVentingStateOption(s)));
                                } else if (OPTION_HOOD_INTENSIVE_LEVEL.equalsIgnoreCase(option.getKey())) {
                                    option.getAllowedValues().stream()
                                            .filter(s -> !STAGE_INTENSIVE_STAGE_OFF.equalsIgnoreCase(s))
                                            .forEach(s -> stateOptions.add(createVentingStateOption(s)));
                                }
                            });
                        } catch (CommunicationException | ApplianceOfflineException | AuthorizationException e) {
                            logger.warn("Could not fetch hood program options. error={}", e.getMessage());
                            stateOptions.add(createVentingStateOption(STAGE_FAN_STAGE_01));
                            stateOptions.add(createVentingStateOption(STAGE_FAN_STAGE_02));
                            stateOptions.add(createVentingStateOption(STAGE_FAN_STAGE_03));
                            stateOptions.add(createVentingStateOption(STAGE_FAN_STAGE_04));
                            stateOptions.add(createVentingStateOption(STAGE_FAN_STAGE_05));
                            stateOptions.add(createVentingStateOption(STAGE_INTENSIVE_STAGE_1));
                            stateOptions.add(createVentingStateOption(STAGE_INTENSIVE_STAGE_2));
                        }
                    }
                });
                stateOptions.add(new StateOption(COMMAND_STOP, "Stop"));

                @Nullable
                StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern("%s")
                        .withReadOnly(stateOptions.isEmpty()).withOptions(stateOptions).build().toStateDescription();

                if (stateDescription != null && !stateOptions.isEmpty()) {
                    getThingChannel(CHANNEL_HOOD_ACTIONS_STATE)
                            .ifPresent(channel -> getDynamicStateDescriptionProvider()
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

    @Override
    protected void removeSelectedProgramStateDescription() {
        getThingChannel(CHANNEL_HOOD_ACTIONS_STATE).ifPresent(channel -> getDynamicStateDescriptionProvider()
                .removeStateDescriptions(channel.getUID().getAsString()));
    }

    @Override
    public String toString() {
        return "HomeConnectHoodHandler [haId: " + getThingHaId() + "]";
    }

    @Override
    protected void resetProgramStateChannels() {
        super.resetProgramStateChannels();
        getThingChannel(CHANNEL_ACTIVE_PROGRAM_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_HOOD_INTENSIVE_LEVEL).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
        getThingChannel(CHANNEL_HOOD_VENTING_LEVEL).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
    }

    private StateOption createVentingStateOption(String optionKey) {
        String label = mapStringType(PROGRAM_HOOD_VENTING);

        if (STAGE_FAN_STAGE_01.equalsIgnoreCase(optionKey)) {
            return new StateOption(COMMAND_VENTING_1,
                    format("%s (Level %s)", label, mapStageStringType(STAGE_FAN_STAGE_01)));
        } else if (STAGE_FAN_STAGE_02.equalsIgnoreCase(optionKey)) {
            return new StateOption(COMMAND_VENTING_2,
                    format("%s (Level %s)", label, mapStageStringType(STAGE_FAN_STAGE_02)));
        } else if (STAGE_FAN_STAGE_03.equalsIgnoreCase(optionKey)) {
            return new StateOption(COMMAND_VENTING_3,
                    format("%s (Level %s)", label, mapStageStringType(STAGE_FAN_STAGE_03)));
        } else if (STAGE_FAN_STAGE_04.equalsIgnoreCase(optionKey)) {
            return new StateOption(COMMAND_VENTING_4,
                    format("%s (Level %s)", label, mapStageStringType(STAGE_FAN_STAGE_04)));
        } else if (STAGE_FAN_STAGE_05.equalsIgnoreCase(optionKey)) {
            return new StateOption(COMMAND_VENTING_5,
                    format("%s (Level %s)", label, mapStageStringType(STAGE_FAN_STAGE_05)));
        } else if (STAGE_INTENSIVE_STAGE_1.equalsIgnoreCase(optionKey)) {
            return new StateOption(COMMAND_VENTING_INTENSIVE_1,
                    format("%s (Intensive level %s)", label, mapStageStringType(STAGE_INTENSIVE_STAGE_1)));
        } else {
            return new StateOption(COMMAND_VENTING_INTENSIVE_2,
                    format("%s (Intensive level %s)", label, mapStageStringType(STAGE_INTENSIVE_STAGE_2)));
        }
    }
}
