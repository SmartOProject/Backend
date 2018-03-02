package com.alarislabs.invoice.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggerLevel {

    private static final Logger Log = LoggerFactory.getLogger(LoggerLevel.class);

    static void setLevel(Level level) {

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("com.alarislabs");

        loggerConfig.setLevel(level);
        ctx.updateLoggers();

        Log.info("Logger level set to " + level.toString());

    }


}
