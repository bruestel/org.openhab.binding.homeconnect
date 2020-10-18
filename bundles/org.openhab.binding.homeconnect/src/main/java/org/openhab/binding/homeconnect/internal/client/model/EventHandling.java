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
package org.openhab.binding.homeconnect.internal.client.model;

import org.eclipse.jdt.annotation.Nullable;

public enum EventHandling {
    NONE("none"),
    ACKNOWLEDGE("acknowledge"),
    DECISION("decision");

    private final String handling;

    EventHandling(String handling) {
        this.handling = handling;
    }

    public String getHandling() {
        return this.handling;
    }

    public static @Nullable EventHandling valueOfHandling(String type) {
        for (EventHandling eventType : EventHandling.values()) {
            if (eventType.handling.equalsIgnoreCase(type)) {
                return eventType;
            }
        }
        return null;
    }
}
