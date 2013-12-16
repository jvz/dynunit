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
