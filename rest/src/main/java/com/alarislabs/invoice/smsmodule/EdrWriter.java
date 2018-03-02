package com.alarislabs.invoice.smsmodule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdrWriter {

    private static final Logger instance = LoggerFactory.getLogger(EdrWriter.class);

    static void writeEdr(String edrAsCsv) {
        instance.trace(edrAsCsv);
    }
}
