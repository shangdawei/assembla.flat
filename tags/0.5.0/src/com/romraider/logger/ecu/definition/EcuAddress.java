package com.romraider.logger.ecu.definition;

public interface EcuAddress {

    String[] getAddresses();

    byte[] getBytes();

    int getBit();

    int getLength();
}
