DynUnit
=======

Fork of ATG DUST for use with JUnit4 and various newer libraries.

Installation
------------

First, you'll need to make some symlinks to your local ATG JARs. Suppose your
`ATG_HOME` environement variable is set to something like
`$HOME/ATG/ATG10.1.2`. Then you should execute the following:

```bash
mkdir lib
cd lib
for module in DAS DPS DSS; do
    ln -s $ATG_HOME/$module/lib/classes.jar $module.jar
    ln -s $ATG_HOME/$module/lib/resources.jar $module-resources.jar
done
```

Next, build the DynUnit JAR file for use in your ATG module:

```bash
./gradlew jar
cp build/libs/dynunit-1.0-SNAPSHOT.jar your/project/libs/
```

Usage
-----

A JUnit test runner is still in development, but DynUnit can be invoked without a special test runner using mostly
standard annotations. Any field which you wish to resolve a Nucleus component to must be annotated with
`@javax.inject.Inject`, and a class must add a `@atg.tools.dynunit.Nuke` annotation to an injected `Nucleus`
instance. Named components are specified using the `@javax.inject.Named` annotation with their component path. For
example:

```java
import atg.nucleus.Nucleus;
import atg.nucleus.logging.PrintStreamLogger;

import org.junit.*;
import atg.tools.dynunit.*;
import javax.inject.*;

public class DynUnitTest {

    // we inject a Nucleus using the base config path specified
    @Inject @Nuke("src/test/resources/config")
    private Nucleus nucleus;

    // by default, Nucleus always creates this component
    @Inject @Named("/atg/dynamo/service/logging/ScreenLog")
    private PrintStreamLogger screenLog;

    @Before
    public void setUp() {
        // this injects the instances marked above
        DynUnit.init(this);
    }

    @After
    public void tearDown() {
        // this stops the Nucleus service
        DynUnit.stop(this);
    }

    @Test
    public void testScreenLogDisabled() {
        Assert.assertFalse(screenLog.isLoggingEnabled());
    }
}
```