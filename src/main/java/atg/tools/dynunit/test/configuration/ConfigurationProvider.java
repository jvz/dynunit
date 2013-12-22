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

package atg.tools.dynunit.test.configuration;

import java.io.File;
import java.io.IOException;

/**
 * @author msicker
 * @version 1.0.0
 */
public abstract class ConfigurationProvider {

    private File root;
    private boolean debug;

    public ConfigurationProvider() {
    }

    public ConfigurationProvider(final File root) {
        this.root = root;
    }

    public ConfigurationProvider(final boolean debug) {
        this.debug = debug;
    }

    public ConfigurationProvider(final File root, final boolean debug) {
        this.root = root;
        this.debug = debug;
    }

    public abstract void createPropertiesByConfigurationLocation()
            throws IOException;

    public File getRoot() {
        return root;
    }

    public void setRoot(final File root) {
        this.root = root;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }
}
