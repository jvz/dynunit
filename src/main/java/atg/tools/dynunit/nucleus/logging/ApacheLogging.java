/*
 * Copyright 2013 Matt Sicker and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package atg.tools.dynunit.nucleus.logging;

import atg.nucleus.logging.TraceApplicationLogging;
import atg.nucleus.logging.VariableArgumentApplicationLogging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.apache.logging.log4j.message.LocalizedMessage;

import java.util.ResourceBundle;

/**
 * Implementation of ApplicationLogging including trace level, variable argument methods, and i18n resource bundles.
 * Note that this implementation of VariableArgumentApplicationLogging does <em>not</em> use the same logic as the
 * bloated version that comes with ATG that supports its own Unified EL-like formatting string. Instead, this uses
 * the {@link org.apache.logging.log4j.message.FormattedMessageFactory} class to figure out how to apply arguments
 * to a format string. Naturally, this can be overridden by constructing an instance using a given Logger object to
 * wrap.
 *
 * @author msicker
 * @version 1.0.0
 */
public class ApacheLogging
        implements VariableArgumentApplicationLogging, TraceApplicationLogging {

    private Logger logger;
    private boolean loggingTrace;
    private boolean loggingDebug;
    private boolean loggingInfo;
    private boolean loggingWarning;
    private boolean loggingError;

    public ApacheLogging() {
        logger = LogManager.getRootLogger();
    }

    public ApacheLogging(final String name) {
        logger = LogManager.getLogger(name, new FormattedMessageFactory());
    }

    public ApacheLogging(final Class<?> datClass) {
        logger = LogManager.getLogger(datClass, new FormattedMessageFactory());
    }

    public ApacheLogging(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isLoggingTrace() {
        return loggingTrace || logger.isTraceEnabled();
    }

    @Override
    public void setLoggingTrace(final boolean loggingTrace) {
        this.loggingTrace = loggingTrace;
    }

    @Override
    public void logTrace(final String message) {
        logger.trace(message);
    }

    @Override
    public void logTrace(final Throwable throwable) {
        logger.trace("", throwable);
    }

    @Override
    public void logTrace(final String message, final Throwable throwable) {
        logger.trace(message, throwable);
    }

    @Override
    public void vlogTrace(final Throwable throwable,
                          final ResourceBundle resourceBundle,
                          final String key,
                          final Object... args) {
        logger.trace(new LocalizedMessage(resourceBundle, key, args), throwable);
    }

    @Override
    public void vlogTrace(final ResourceBundle resourceBundle, final String key, final Object... args) {
        logger.trace(new LocalizedMessage(resourceBundle, key, args));
    }

    @Override
    public void vlogTrace(final Throwable throwable, final String format, final Object... args) {
        logTrace(throwable);
        logger.trace(format, args);
    }

    @Override
    public void vlogTrace(final String format, final Object... args) {
        logger.trace(format, args);
    }

    @Override
    public boolean isLoggingDebug() {
        return loggingDebug || logger.isDebugEnabled();
    }

    @Override
    public void setLoggingDebug(final boolean loggingDebug) {
        this.loggingDebug = loggingDebug;
    }

    @Override
    public void logDebug(final String message) {
        logger.debug(message);
    }

    @Override
    public void logDebug(final Throwable throwable) {
        logger.debug("", throwable);
    }

    @Override
    public void logDebug(final String message, final Throwable throwable) {
        logger.debug(message, throwable);
    }

    @Override
    public void vlogDebug(final Throwable throwable,
                          final ResourceBundle resourceBundle,
                          final String key,
                          final Object... args) {
        logger.debug(new LocalizedMessage(resourceBundle, key, args), throwable);
    }

    @Override
    public void vlogDebug(final ResourceBundle resourceBundle, final String key, final Object... args) {
        logger.debug(new LocalizedMessage(resourceBundle, key, args));
    }

    @Override
    public void vlogDebug(final Throwable throwable, final String format, final Object... args) {
        logDebug(throwable);
        logger.debug(format, args);
    }

    @Override
    public void vlogDebug(final String format, final Object... args) {
        logger.debug(format, args);
    }

    @Override
    public boolean isLoggingInfo() {
        return loggingInfo || logger.isInfoEnabled();
    }

    @Override
    public void setLoggingInfo(final boolean loggingInfo) {
        this.loggingInfo = loggingInfo;
    }

    @Override
    public void logInfo(final String message) {
        logger.info(message);
    }

    @Override
    public void logInfo(final Throwable throwable) {
        logger.info("", throwable);
    }

    @Override
    public void logInfo(final String message, final Throwable throwable) {
        logger.info(message, throwable);
    }

    @Override
    public void vlogInfo(final Throwable throwable,
                         final ResourceBundle resourceBundle,
                         final String key,
                         final Object... args) {
        logger.info(new LocalizedMessage(resourceBundle, key, args), throwable);
    }

    @Override
    public void vlogInfo(final ResourceBundle resourceBundle, final String key, final Object... args) {
        logger.info(new LocalizedMessage(resourceBundle, key, args));
    }

    @Override
    public void vlogInfo(final Throwable throwable, final String format, final Object... args) {
        logInfo(throwable);
        logger.info(format, args);
    }

    @Override
    public void vlogInfo(final String format, final Object... args) {
        logger.info(format, args);
    }

    @Override
    public boolean isLoggingWarning() {
        return loggingWarning || logger.isWarnEnabled();
    }

    @Override
    public void setLoggingWarning(final boolean loggingWarning) {
        this.loggingWarning = loggingWarning;
    }

    @Override
    public void logWarning(final String message) {
        logger.warn(message);
    }

    @Override
    public void logWarning(final Throwable throwable) {
        logger.warn("", throwable);
    }

    @Override
    public void logWarning(final String message, final Throwable throwable) {
        logger.warn(message, throwable);
    }

    @Override
    public void vlogWarning(final Throwable throwable,
                            final ResourceBundle resourceBundle,
                            final String key,
                            final Object... args) {
        logger.warn(new LocalizedMessage(resourceBundle, key, args), throwable);
    }

    @Override
    public void vlogWarning(final ResourceBundle resourceBundle, final String key, final Object... args) {
        logger.warn(new LocalizedMessage(resourceBundle, key, args));
    }

    @Override
    public void vlogWarning(final Throwable throwable, final String format, final Object... args) {
        logWarning(throwable);
        logger.warn(format, args);
    }

    @Override
    public void vlogWarning(final String format, final Object... args) {
        logger.warn(format, args);
    }

    @Override
    public boolean isLoggingError() {
        return loggingError || logger.isErrorEnabled();
    }

    @Override
    public void setLoggingError(final boolean loggingError) {
        this.loggingError = loggingError;
    }

    @Override
    public void logError(final String message) {
        logger.error(message);
    }

    @Override
    public void logError(final Throwable throwable) {
        logger.error("", throwable);
    }

    @Override
    public void logError(final String message, final Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public void vlogError(final Throwable throwable,
                          final ResourceBundle resourceBundle,
                          final String key,
                          final Object... args) {
        logger.error(new LocalizedMessage(resourceBundle, key, args), throwable);
    }

    @Override
    public void vlogError(final ResourceBundle resourceBundle, final String key, final Object... args) {
        logger.error(new LocalizedMessage(resourceBundle, key, args));
    }

    @Override
    public void vlogError(final Throwable throwable, final String format, final Object... args) {
        logError(throwable);
        logger.error(format, args);
    }

    @Override
    public void vlogError(final String format, final Object... args) {
        logger.error(format, args);
    }
}
