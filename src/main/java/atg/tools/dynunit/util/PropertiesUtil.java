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

package atg.tools.dynunit.util;

import atg.nucleus.DynamoEnv;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author msicker
 * @version 1.0.0
 */
public final class PropertiesUtil {

    private static final Logger logger = LogManager.getLogger();

    private PropertiesUtil() {
    }

    public static String getSystemProperty(final String key) {
        logger.entry(key);
        return logger.exit(System.getProperty(key));
    }

    public static void setSystemProperty(final String key, final String value) {
        logger.entry(key, value);
        System.setProperty(key, value);
        logger.exit();
    }

    public static void setSystemPropertyIfEmpty(final String key, final String value) {
        logger.entry(key, value);
        if (StringUtils.isEmpty(getSystemProperty(key))) {
            setSystemProperty(key, value);
        }
        logger.exit();
    }

    public static String getDynamoProperty(final String key) {
        logger.entry(key);
        return logger.exit(DynamoEnv.getProperty(key));
    }

    public static void setDynamoProperty(final String key, final String value) {
        logger.entry(key, value);
        DynamoEnv.setProperty(key, value);
        logger.exit();
    }

    public static void setDynamoPropertyIfEmpty(final String key, final String value) {
        logger.entry(key, value);
        if (StringUtils.isEmpty(getDynamoProperty(key))) {
            setDynamoProperty(key, value);
        }
        logger.exit();
    }

}
