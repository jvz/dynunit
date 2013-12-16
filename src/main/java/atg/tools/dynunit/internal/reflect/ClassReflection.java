package atg.tools.dynunit.internal.reflect;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for scanning annotated elements in a class. Because reflection is an expensive operation in Java (sigh),
 * the caller must call initFields(), initMethods(), or init() to initialize the fields, methods, or both, respectively.
 * <p/>
 * This class is not a part of the public API and is subject to change. It is used for finding injection points. It is
 * also used for caching annotations and such for faster lookup afterward.
 *
 * @author msicker
 * @version 1.0.0
 */
public class ClassReflection<T> {
    // TODO: cache annotation types

    private static final Logger logger = LogManager.getLogger();

    private final T object;
    private final Class<T> klass;
    private Field[] fields = null;
    private Method[] methods = null;

    @SuppressWarnings("unchecked") // seriously Java, get your generic shit together
    public ClassReflection(@NotNull final T object) {
        this.object = object;
        klass = (Class<T>) object.getClass();
    }

    public void init() {
        logger.entry();
        initFields();
        initMethods();
        logger.exit();
    }

    public void initFields() {
        logger.entry();
        fields = klass.getFields();
        logger.exit();
    }

    public void initMethods() {
        logger.entry();
        methods = klass.getMethods();
        logger.exit();
    }

    @NotNull
    public T getObject() {
        logger.entry();
        return logger.exit(object);
    }

    @NotNull
    public Class<T> getObjectClass() {
        logger.entry();
        return logger.exit(klass);
    }

    @Nullable
    public <A extends Annotation> Pair<? extends AnnotatedElement, A> getAnnotatedElement(@NotNull final Class<A> annotationClass) {
        logger.entry(annotationClass);
        A annotation = getClassAnnotation(annotationClass);
        if (annotation != null) {
            return logger.exit(Pair.of(klass, annotation));
        }
        for (final Field field : fields) {
            annotation = getFieldAnnotation(field, annotationClass);
            if (annotation != null) {
                return logger.exit(Pair.of(field, annotation));
            }
        }
        for (final Method method : methods) {
            annotation = getMethodAnnotation(method, annotationClass);
            if (annotation != null) {
                return logger.exit(Pair.of(method, annotation));
            }
        }
        return logger.exit(null);
    }

    public Annotation[] getClassAnnotations() {
        logger.entry();
        final Annotation[] annotations = klass.getAnnotations();
        return logger.exit(annotations);
    }

    @Nullable
    public <A extends Annotation> A getClassAnnotation(@NotNull final Class<A> annotationClass) {
        return getElementAnnotation(klass, annotationClass);
    }

    public Field[] getFields() {
        logger.entry();
        return logger.exit(fields);
    }

    @NotNull
    public Map<Field, Annotation[]> getFieldAnnotations() {
        return getElementalAnnotations(fields);
    }

    @Nullable
    public <A extends Annotation> A getFieldAnnotation(@NotNull final Field field,
                                                       @NotNull final Class<A> annotationClass) {
        return getElementAnnotation(field, annotationClass);
    }

    public Method[] getMethods() {
        logger.entry();
        return logger.exit(methods);
    }

    @NotNull
    public Map<Method, Annotation[]> getMethodAnnotations() {
        return getElementalAnnotations(methods);
    }

    @Nullable
    public <A extends Annotation> A getMethodAnnotation(@NotNull final Method method,
                                                        @NotNull final Class<A> annotationClass) {
        return getElementAnnotation(method, annotationClass);
    }

    @Nullable
    private static <E extends AnnotatedElement, A extends Annotation> A getElementAnnotation(@NotNull E element,
                                                                                             @NotNull Class<A> annotationClass) {
        logger.entry(element, annotationClass);
        final A annotation = element.getAnnotation(annotationClass);
        return logger.exit(annotation);
    }

    @NotNull
    private static <E extends AnnotatedElement> Map<E, Annotation[]> getElementalAnnotations(@NotNull E[] elements) {
        logger.entry((Object[]) elements);
        final Map<E, Annotation[]> elementAnnotations = new ConcurrentHashMap<E, Annotation[]>();
        for (final E element : elements) {
            elementAnnotations.put(element, element.getAnnotations());
        }
        return logger.exit(elementAnnotations);
    }
}
