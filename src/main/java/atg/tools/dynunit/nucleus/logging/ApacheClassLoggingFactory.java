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

import atg.nucleus.InsertableGenericContext;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;

/**
 * ClassLoggingFactory implementation that uses custom ApacheLogging instances. This class should be used for the
 * component {@code /atg/dynamo/service/logging/ClassLoggingFactory}. Either way, once this class is initialized, it
 * will set the default ClassLoggingFactory to an instance of this class.
 *
 * @author msicker
 * @version 1.0.0
 */
public class ApacheClassLoggingFactory
        extends InsertableGenericContext
        implements ClassLoggingFactory.Factory {

    static {
        ClassLoggingFactory.setFactory(new ApacheClassLoggingFactory());
    }

    @Override
    public ApplicationLogging getLoggerForClass(final Class datClass) {
        return new ApacheLogging(datClass);
    }

    @Override
    public ApplicationLogging getLoggerForClassName(final String className) {
        return new ApacheLogging(className);
    }
}
