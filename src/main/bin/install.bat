@echo on
setlocal

REM
REM ATG DUST Mavn Repository Installation
REM 
REM This script installs required ATG libraries to a local maven
REM repository for DUST development.
REM
REM $Id: $

title Installing ATG DUST

goto :start

:nomavenhome
echo Apache Maven2 does not appear to be installed. It's required to build ATG DUST. Please install it, set MAVEN_HOME and try again.
goto :end

:start

if DEFINED DYNAMO_HOME goto :checkdusthome
REM TODO Be nicer and prompt for ATG installation
echo DYNAMO_HOME is not set. Please set it. For example: set DYNAMO_HOME=C:\ATG\ATG9.1\home
goto :end

:checkdusthome
if DEFINED DUST_HOME goto :homeset
echo DUST_HOME is not set. Please set it. DUST home is the location in which you have checked out
echo DUST from svn. If you are using a build, this script is not required. It is only for installing
echo libraries into a maven repository.
goto :end

REM Use DYNAMO_HOME that we found in the environment
:homeset
set USE_DYNAMO_HOME=%DYNAMO_HOME%

if DEFINED MAVEN_HOME goto :mavenhomeset

:mavenhomeset


REM Ok Need to install DAS/lib/classes.jar and DAS/lib/resources.jar
REM Assuming 9.1
echo Installing DAS/lib/classes.jar

cd %DUST_HOME%
call %MAVEN_HOME%\bin\mvn deploy:deploy-file -DgroupId=atg -DartifactId=das -Dversion=9.1 -Dpackaging=jar -Dfile=%DYNAMO_HOME%/../DAS/lib/classes.jar -DgeneratePom=true -Durl=file://c:/maven/repository -DlocalRepository=local

call %MAVEN_HOME%\bin\mvn deploy:deploy-file -DgroupId=atg -DartifactId=das-resources -Dversion=9.1 -Dpackaging=jar -Dfile=%DYNAMO_HOME%/../DAS/lib/resources.jar -DgeneratePom=true -Durl=file://c:/maven/repository -DlocalRepository=local

REM
REM DPS
REM

call %MAVEN_HOME%\bin\mvn deploy:deploy-file -DgroupId=atg -DartifactId=dps -Dversion=9.1 -Dpackaging=jar -Dfile=%DYNAMO_HOME%/../DPS/lib/classes.jar -DgeneratePom=true -Durl=file://c:/maven/repository -DlocalRepository=local

call %MAVEN_HOME%\bin\mvn deploy:deploy-file -DgroupId=atg -DartifactId=dps-resources -Dversion=9.1 -Dpackaging=jar -Dfile=%DYNAMO_HOME%/../DPS/lib/resources.jar -DgeneratePom=true -Durl=file://c:/maven/repository -DlocalRepository=local

REM
REM DSS
REM

call %MAVEN_HOME%\bin\mvn deploy:deploy-file -DgroupId=atg -DartifactId=dss -Dversion=9.1 -Dpackaging=jar -Dfile=%DYNAMO_HOME%/../DSS/lib/classes.jar -DgeneratePom=true -Durl=file://c:/maven/repository -DlocalRepository=local

call %MAVEN_HOME%\bin\mvn deploy:deploy-file -DgroupId=atg -DartifactId=dss-resources -Dversion=9.1 -Dpackaging=jar -Dfile=%DYNAMO_HOME%/../DSS/lib/resources.jar -DgeneratePom=true -Durl=file://c:/maven/repository -DlocalRepository=local



echo ATG libraries installed. You may now build ATG DUST by typing 'mvn install'.

:end
