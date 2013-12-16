package atg.tools.dynunit.internal.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author msicker
 * @version 1.0.0
 */
@Documented
@Inherited
@Target({ TYPE, FIELD, METHOD })
@Retention(RUNTIME)
public @interface Fake {

    public String value() default "";
}
