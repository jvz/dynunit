package atg.tools.dynunit;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Injection qualifier for a Nucleus instance. This annotation should be used on the test class itself or on a
 * {@link atg.nucleus.Nucleus Nucleus} field. The class should use {@link atg.tools.dynunit.DynUnit#init(Object)} to
 * inject the Nucleus along with any Nucleus-registered components.
 *
 * @author msicker
 * @version 1.0.0
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, FIELD })
@Qualifier
@Inherited
public @interface Nuke {

    /**
     * Root ATG-Config-Path to use for Nucleus instance.
     */
    public String value();

    public String[] modules() default { };
}
