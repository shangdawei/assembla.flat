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

package com.romraider.io.serial.connection;

import com.romraider.io.connection.ConnectionProperties;
import com.romraider.logger.ecu.exception.NotConnectedException;
import com.romraider.logger.ecu.exception.PortNotFoundException;
import com.romraider.logger.ecu.exception.SerialCommunicationException;
import com.romraider.logger.ecu.exception.UnsupportedPortTypeException;
import static com.romraider.util.HexUtil.asHex;
import static com.romraider.util.ParamChecker.checkNotNull;
import static com.romraider.util.ParamChecker.checkNotNullOrEmpty;
import static com.romraider.util.ThreadUtil.sleep;
import gnu.io.CommPortIdentifier;
import static gnu.io.CommPortIdentifier.PORT_SERIAL;
import static gnu.io.CommPortIdentifier.getPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import static gnu.io.SerialPort.FLOWCONTROL_NONE;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.Logger;
import static org.apache.log4j.Logger.getLogger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public final class SerialConnectionImpl implements SerialConnection {
    private static final Logger LOGGER = getLogger(SerialConnectionImpl.class);
    private final SerialPort serialPort;
    private final BufferedOutputStream os;
    private final BufferedInputStream is;
    private final BufferedReader reader;

    public SerialConnectionImpl(String portName, ConnectionProperties connectionProperties) {
        checkNotNullOrEmpty(portName, "portName");
        checkNotNull(connectionProperties, "connectionProperties");
        serialPort = connect(portName, connectionProperties);
        os = initOutputStream(serialPort);
        is = initInputStream(serialPort);
        reader = reader(is);
    }

    private BufferedReader reader(BufferedInputStream is) {
        return new BufferedReader(new InputStreamReader(this.is));
    }

    public void write(byte[] bytes) {
        try {
            os.write(bytes, 0, bytes.length);
            os.flush();
        } catch (IOException e) {
            close();
            throw new SerialCommunicationException(e);
        }
    }

    public int available() {
        try {
            return is.available();
        } catch (IOException e) {
            close();
            throw new SerialCommunicationException(e);
        }
    }

    public int read() {
        try {
            waitForBytes(1);
            return is.read();
        } catch (IOException e) {
            close();
            throw new SerialCommunicationException(e);
        }
    }

    public void read(byte[] bytes) {
        try {
            waitForBytes(bytes.length);
            is.read(bytes, 0, bytes.length);
        } catch (IOException e) {
            close();
            throw new SerialCommunicationException(e);
        }
    }

    public String readLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            close();
            throw new SerialCommunicationException(e);
        }
    }

    public byte[] readAvailable() {
        byte[] response = new byte[available()];
        read(response);
        return response;
    }

    public void readStaleData() {
        if (available() <= 0) return;
        byte[] staleBytes = readAvailable();
        LOGGER.debug("Stale data read: " + asHex(staleBytes));
    }

    public void close() {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                LOGGER.error("Error closing output stream", e);
            }
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.error("Error closing input stream reader", e);
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                LOGGER.error("Error closing input stream", e);
            }
        }
        if (serialPort != null) {
            try {
                serialPort.close();
            } catch (Exception e) {
                LOGGER.error("Error closing serial port", e);
            }
        }
        LOGGER.info("Connection closed.");
    }

    private BufferedOutputStream initOutputStream(SerialPort serialPort) {
        try {
            OutputStream os = serialPort.getOutputStream();
            return new BufferedOutputStream(os);
        } catch (IOException e) {
            close();
            throw new NotConnectedException(e);
        }
    }

    private BufferedInputStream initInputStream(SerialPort serialPort) {
        try {
            InputStream is = serialPort.getInputStream();
            return new BufferedInputStream(is);
        } catch (IOException e) {
            close();
            throw new NotConnectedException(e);
        }
    }

    private SerialPort connect(String portName, ConnectionProperties connectionProperties) {
        CommPortIdentifier portIdentifier = resolvePortIdentifier(portName);
        SerialPort serialPort = openPort(portIdentifier, connectionProperties.getConnectTimeout());
        initSerialPort(serialPort, connectionProperties.getBaudRate(), connectionProperties.getDataBits(), connectionProperties.getStopBits(),
                connectionProperties.getParity());
        LOGGER.info("Connected to: " + portName);
        return serialPort;
    }

    private SerialPort openPort(CommPortIdentifier portIdentifier, int connectTimeout) {
        checkIsSerialPort(portIdentifier);
        try {
            return (SerialPort) portIdentifier.open(this.getClass().getName(), connectTimeout);
        } catch (PortInUseException e) {
            throw new SerialCommunicationException("Port is currently in use: " + portIdentifier.getName());
        }
    }

    private void checkIsSerialPort(CommPortIdentifier portIdentifier) {
        if (portIdentifier.getPortType() != PORT_SERIAL) {
            throw new UnsupportedPortTypeException("Port type " + portIdentifier.getPortType() + " not supported - must be serial.");
        }
    }

    private void initSerialPort(SerialPort serialPort, int baudrate, int dataBits, int stopBits, int parity) {
        try {
            serialPort.setFlowControlMode(FLOWCONTROL_NONE);
            serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
            serialPort.disableReceiveTimeout();
            serialPort.setRTS(false);
        } catch (UnsupportedCommOperationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private CommPortIdentifier resolvePortIdentifier(String portName) {
        try {
            return getPortIdentifier(portName);
        } catch (NoSuchPortException e) {
            throw new PortNotFoundException("Unable to resolve port: " + portName);
        }
    }

    private void waitForBytes(int numBytes) {
        while (available() < numBytes) sleep(2L);
    }
}
