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

package atg.tools.dynunit.hamcrest;

import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.lang.annotation.Annotation;

import static org.hamcrest.object.IsCompatibleType.typeCompatibleWith;

/**
 * Simple Hamcrest feature matcher that provides matching against annotation types.
 *
 * @author msicker
 * @version 1.0.0
 */
public class IsAnnotated<A extends Annotation>
        extends FeatureMatcher<A, Class<A>> {

    protected IsAnnotated(final Matcher<? super Class<A>> subMatcher) {
        super(subMatcher, "is annotated", "Annotated");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<A> featureValueOf(final A actual) {
        return (Class<A>) actual.annotationType();
    }

    @Factory
    public static <T extends Annotation> Matcher<T> annotated(final T annotation) {
        return new IsAnnotated<T>(typeCompatibleWith(annotation.annotationType()));
    }

    @Factory
    public static <T extends Annotation> Matcher<T> annotated(final Class<? extends Annotation> annotationType) {
        return new IsAnnotated<T>(typeCompatibleWith(annotationType));
    }
}
