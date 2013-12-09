package atg.tools.dynunit;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Injection annotation marking a field to be injected with a Nucleus instance. Unit tests using this annotation on a
 * public Nucleus field should initialize the injections via {@code DynUnit.init(this)}.
 *
 * @author msicker
 * @version 1.0.0
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
@Qualifier
public @interface Nuke {

    /**
     * Root ATG-Config-Path to use for Nucleus instance.
     */
    public String value();
}
