package atg.tools.dynunit.internal.inject;

import atg.tools.dynunit.internal.reflect.ClassReflection;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matcher;
import org.hamcrest.core.AnyOf;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static atg.tools.dynunit.hamcrest.IsAnnotated.annotated;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.typeCompatibleWith;
import static org.junit.Assert.assertNotNull;

/**
 * @author msicker
 * @version 1.0.0
 */
public class ClassReflectionTest {

    private AnnotatedClass object;
    private ClassReflection<AnnotatedClass> reflection;

    @Before
    public void setUp()
            throws Exception {
        object = new AnnotatedClass();
        reflection = new ClassReflection<AnnotatedClass>(object);
        reflection.init();
    }

    @Test
    public void testGetObject()
            throws Exception {
        assertThat(reflection.getObject(), is(sameInstance(object)));
    }

    @Test
    @SuppressWarnings("unchecked") // again, Java, get it together
    public void testGetClass()
            throws Exception {
        assertThat(reflection.getObjectClass(), is(equalTo((Class<AnnotatedClass>) object.getClass())));
    }

    @Test
    public void testGetElementForAnnotationAny()
            throws Exception {
        final Pair<? extends AnnotatedElement, Any> annotatedElement = reflection.getAnnotatedElement(Any.class);
        assertNotNull(annotatedElement);
        assertThat(
                "The first (and only) element annotated @Any should be the class itself.",
                annotatedElement.getLeft(),
                is(instanceOf(Class.class))
        );
    }

    @Test
    public void testGetElementForAnnotationDefault()
            throws Exception {
        final Pair<? extends AnnotatedElement, Default> annotatedElement = reflection.getAnnotatedElement(Default.class);
        assertNotNull(annotatedElement);
        assertThat(
                "The only element annotated @Default should be the method setReal",
                annotatedElement.getLeft(),
                is(instanceOf(Method.class))
        );
        final Method setReal = (Method) annotatedElement.getLeft();
        assertThat(setReal.getName(), is(equalTo("setReal")));
    }

    @Test
    public void testGetClassAnnotations()
            throws Exception {
        final List<Annotation> annotations = Arrays.asList(reflection.getClassAnnotations());
        assertNotNull(annotations);
        for (Annotation annotation : annotations) {
            assertAnnotationIsAnyOf(annotation, Any.class, Fake.class);
        }
        assertThat(annotations, containsInAnyOrder(annotated(Any.class), annotated(Fake.class)));
    }

    private void assertAnnotationIsAnyOf(final Annotation annotation, final Class<?>... types) {
        final List<Matcher<? super Class<?>>> matchers = new ArrayList<Matcher<? super Class<?>>>(types.length);
        for (Class<?> type : types) {
            matchers.add(typeCompatibleWith(type));
        }
        final AnyOf<Class<?>> anyAnnotation = new AnyOf<Class<?>>(matchers);
        assertThat(annotation.annotationType(), is(anyAnnotation));
    }
}
