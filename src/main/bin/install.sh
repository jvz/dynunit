#!/bin/sh

###################
# Make sure Apache Maven2 is installed
# $Id: $
###################

checkMavenHome()
{
  if [ ! -f ${MAVEN_HOME}/bin/mvn ] ; then
   echo "Apache Maven 2 was not found at the location specified by the MAVEN_HOME variable. Please make sure MAVEN_HOME is set correctly".
   exit 1
  fi
}

###################
# Make sure DUST_HOME variable is set
###################

checkDustHome() 
{
 if [ "${DUST_HOME}" = "" ] ; then
  echo "You must set the DUST_HOME environment variable to point to "
  echo "into which you checked out DUST from subversion."
  DYNAMO_HOME=.
  DYNAMO_ROOT=..
  exit 1
 fi

}

###################
# Make sure the DYNAMO_HOME variable is set
###################

checkDynamoHome()
{
  if [ "${DYNAMO_HOME}" = "" ] ; then
    echo "You must set the DYNAMO_HOME environment variable to point to your"
    echo "ATG install directory."
    DYNAMO_HOME=.
    DYNAMO_ROOT=..
    exit 1
  else
    #
    # Set DYNAMO_ROOT to be the directory above DYNAMO_HOME
    #
  DYNAMO_ROOT=${DYNAMO_HOME}/..
  fi
}

###################
# Actually invoke maven and install libraries to the local repository
###################

doInstall()
{
  pushd ${DUST_HOME}
  ${MAVEN_HOME}/bin/mvn install:install-file -DgroupId=atg -DartifactId=das-resources -Dversion=9.1 -Dpackaging=jar -Dfile=${DYNAMO_HOME}/../DAS/lib/resources.jar
  ${MAVEN_HOME}/bin/mvn install:install-file -DgroupId=atg -DartifactId=das -Dversion=9.1 -Dpackaging=jar -Dfile=${DYNAMO_HOME}/../DAS/lib/classes.jar
  ${MAVEN_HOME}/bin/mvn install:install-file -DgroupId=atg -DartifactId=dps -Dversion=9.1 -Dpackaging=jar -Dfile=${DYNAMO_HOME}/../DPS/lib/classes.jar
  ${MAVEN_HOME}/bin/mvn install:install-file -DgroupId=atg -DartifactId=dps-resources -Dversion=9.1 -Dpackaging=jar -Dfile=${DYNAMO_HOME}/../DPS/lib/resources.jar
  ${MAVEN_HOME}/bin/mvn install:install-file -DgroupId=atg -DartifactId=dss -Dversion=9.1 -Dpackaging=jar -Dfile=${DYNAMO_HOME}/../DSS/lib/classes.jar
  ${MAVEN_HOME}/bin/mvn install:install-file -DgroupId=atg -DartifactId=dss-resources -Dversion=9.1 -Dpackaging=jar -Dfile=${DYNAMO_HOME}/../DSS/lib/resources.jar
  popd
}

###################
# Prints a success message
###################

success() 
{
 echo "ATG libraries installed. You may now build ATG DUST by typing 'mvn install'."
}

###################
# Main Script
###################

checkMavenHome
checkDustHome
checkDynamoHome
doInstall
success
