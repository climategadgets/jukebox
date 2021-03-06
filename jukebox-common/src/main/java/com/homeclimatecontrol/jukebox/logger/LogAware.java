package com.homeclimatecontrol.jukebox.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Basic log aware object.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 1998-2018
 */
public abstract class LogAware {

    /**
     * The logger to submit the log messages to.
     */
    protected final Logger logger = LogManager.getLogger(getClass());

    protected LogAware() {
    }

    /**
     * @return The current logger.
     */
    public final Logger getLogger() {
        return logger;
    }
}