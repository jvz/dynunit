DynUnit
=======

Fork of ATG DUST for use with JUnit4 and various newer libraries.

## Usage
First, you need to copy a few JARs to your local Maven repository (default is `~/.m2/repository/`).

```bash
export ATG_VERSION=10.1.2
export ATG_HOME=$HOME/ATG/ATG${ATG_VERSION}
mvn deploy:deploy-file -DgroupId=atg -DartifactId=DAS \
  -Dversion=$ATG_VERSION -Dpackaging=jar \
  -Dfile=${ATG_HOME}/DAS/lib/classes.jar -DgeneratePom=true \
  -Durl=file://$HOME/.m2/repository/ -DlocalRepository=local
```

This has to be done for DAS, DPS, and DSS, as well as their corresponding resources JAR files:

```bash
mvn deploy:deploy-file -DgroupId=atg -DartifactId=DAS-resources \
  -Dversion=$ATG_VERSION -Dpackaging=jar \
  -Dfile=${ATG_HOME}/DAS/lib/resources.jar -DgeneratePom=true \
  -Durl=file://$HOME/.m2/repository/ -DlocalRepository=local
```

The `pom.xml` file also needs to be updated with the version numbers in use.

More to come!
