DynUnit
=======

Fork of ATG DUST for use with JUnit4 and various newer libraries.

Usage
-----

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
