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

package atg.tools.dynunit.internal.inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;

/**
 * @author msicker
 * @version 1.0.0
 */
@Fake
@Any
public class AnnotatedClass {

    @Fake
    private Logger logger = LogManager.getLogger();
    @Fake
    private int n;
    @Fake("named")
    private String string;
    private double real = Math.PI;

    @Fake("another name")
    public void anotherName() {
        logger.entry();
        logger.info("Invoked.");
        logger.exit();
    }

    @Fake
    public boolean isTrue() {
        return true;
    }

    public double getReal() {
        return real;
    }

    @Default
    public void setReal(final double real) {
        this.real = real;
    }
}
