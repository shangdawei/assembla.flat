/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2009 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.io.protocol.ssm;

import static com.romraider.io.protocol.ssm.SSMChecksumCalculator.calculateChecksum;
import static com.romraider.io.protocol.ssm.SSMProtocol.DIAGNOSTIC_TOOL_ID;
import static com.romraider.io.protocol.ssm.SSMProtocol.ECU_ID;
import static com.romraider.io.protocol.ssm.SSMProtocol.ECU_INIT_RESPONSE;
import static com.romraider.io.protocol.ssm.SSMProtocol.HEADER;
import static com.romraider.io.protocol.ssm.SSMProtocol.READ_ADDRESS_RESPONSE;
import static com.romraider.io.protocol.ssm.SSMProtocol.READ_MEMORY_RESPONSE;
import static com.romraider.io.protocol.ssm.SSMProtocol.RESPONSE_NON_DATA_BYTES;
import static com.romraider.io.protocol.ssm.SSMProtocol.WRITE_ADDRESS_RESPONSE;
import static com.romraider.io.protocol.ssm.SSMProtocol.WRITE_MEMORY_RESPONSE;
import com.romraider.logger.ecu.exception.InvalidResponseException;
import static com.romraider.util.ByteUtil.asByte;
import static com.romraider.util.HexUtil.asHex;
import static com.romraider.util.ParamChecker.checkNotNull;
import static com.romraider.util.ParamChecker.checkNotNullOrEmpty;

public final class SSMResponseProcessor {

    private SSMResponseProcessor() {
        throw new UnsupportedOperationException();
    }

    public static byte[] filterRequestFromResponse(byte[] request, byte[] response) {
        checkNotNull(request, "request");
        checkNotNullOrEmpty(response, "response");
        //LOGGER.("Raw request        = " + asHex(request));
        //LOGGER.("Raw response       = " + asHex(response));
        byte[] filteredResponse = new byte[response.length - request.length];
        System.arraycopy(response, request.length, filteredResponse, 0, filteredResponse.length);
        //LOGGER.("Filtered response  = " + asHex(filteredResponse));
        //LOGGER.();
        return filteredResponse;
    }

    public static void validateResponse(byte[] response) {
        int i = 0;
        assertTrue(response.length > RESPONSE_NON_DATA_BYTES, "Invalid response length");
        assertEquals(HEADER, response[i++], "Invalid header");
        assertEquals(DIAGNOSTIC_TOOL_ID, response[i++], "Invalid diagnostic tool id");
        assertEquals(ECU_ID, response[i++], "Invalid ECU id");
        assertEquals(asByte(response.length - RESPONSE_NON_DATA_BYTES + 1), response[i++], "Invalid response data length");
        assertOneOf(new byte[]{ECU_INIT_RESPONSE, READ_ADDRESS_RESPONSE, READ_MEMORY_RESPONSE, WRITE_ADDRESS_RESPONSE, WRITE_MEMORY_RESPONSE}, response[i], "Invalid response code");
        assertEquals(calculateChecksum(response), response[response.length - 1], "Invalid checksum");
    }

    public static byte[] extractResponseData(byte[] response) {
        checkNotNullOrEmpty(response, "response");
        // 0x80 0xF0 0x10 data_length 0xE8 response_data checksum
        validateResponse(response);
        byte[] data = new byte[response.length - RESPONSE_NON_DATA_BYTES];
        System.arraycopy(response, (RESPONSE_NON_DATA_BYTES - 1), data, 0, data.length);
        return data;
    }


    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new InvalidResponseException(msg);
        }
    }


    private static void assertEquals(byte expected, byte actual, String msg) {
        if (actual != expected) {
            throw new InvalidResponseException(msg + ". Expected: " + asHex(new byte[]{expected}) + ". Actual: " + asHex(new byte[]{actual}) + ".");
        }
    }

    private static void assertOneOf(byte[] validOptions, byte actual, String msg) {
        for (byte option : validOptions) {
            if (option == actual) {
                return;
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < validOptions.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(asHex(new byte[]{validOptions[i]}));
        }
        throw new InvalidResponseException(msg + ". Expected one of [" + builder.toString() + "]. Actual: " + asHex(new byte[]{actual}) + ".");
    }
}
