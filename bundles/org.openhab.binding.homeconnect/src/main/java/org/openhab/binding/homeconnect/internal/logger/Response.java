/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.homeconnect.internal.logger;

import java.util.HashMap;

/**
 *
 * HTTP response log model.
 *
 * @author Jonas Brüstel - Initial Contribution
 */
public class Response {
    private int code;
    private HashMap<String, String> header;
    private String body;

    public Response(int code, HashMap<String, String> header, String body) {
        super();
        this.code = code;
        this.header = header;
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    public HashMap<String, String> getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "Response [code=" + code + ", header=" + header + ", body=" + body + "]";
    }
}