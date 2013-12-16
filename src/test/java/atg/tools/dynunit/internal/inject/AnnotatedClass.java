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
