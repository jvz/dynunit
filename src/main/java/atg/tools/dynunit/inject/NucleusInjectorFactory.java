package atg.tools.dynunit.inject;

import atg.tools.dynunit.internal.inject.NucleusInjectorImpl;

/**
 * @author msicker
 * @version 1.0.0
 */
public class NucleusInjectorFactory {

    public static NucleusInjector getInjector() {
        return new NucleusInjectorImpl();
    }
}
