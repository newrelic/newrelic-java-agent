#! /bin/bash

if test $# -ne 2
	then
		echo "Usage: $0 version-num s3-profile-name"
		exit 1
	fi

VERSION=$1
S3_PROFILE=$2

#Prod downloads bucket
BASE_S3_URL=nr-downloads-main/newrelic/java-agent


SERVERAGENTDIR=${BASE_S3_URL}/newrelic-agent/${VERSION}
SERVERAPIDIR=${BASE_S3_URL}/newrelic-api/${VERSION}

ZIPNAME=newrelic-java-${VERSION}.zip
AGENT_POM_NAME=newrelic-agent-${VERSION}.pom
AGENT_API_NAME=newrelic-api-${VERSION}.pom
MYPATH=$0
INSTALLDIR="`dirname ${MYPATH}`"

MYDIR=/tmp/$$

MVNDIR=${MYDIR}/sonatype
POMPATH=${MVNDIR}/pom.xml
ZIP=${MVNDIR}/${ZIPNAME}
AGENT_POM=${MVNDIR}/${AGENT_POM_NAME}
API_POM=${MVNDIR}/${AGENT_API_NAME}
mkdir -p ${MVNDIR}

BASE_PUBLIC_DOWNLOAD_URL=https://repo1.maven.org/maven2/com/newrelic/agent/java
AGENT_ZIP_URL=${BASE_PUBLIC_DOWNLOAD_URL}/newrelic-java/${VERSION}/newrelic-java-${VERSION}.zip
AGENT_POM_URL=${BASE_PUBLIC_DOWNLOAD_URL}/newrelic-agent/${VERSION}/newrelic-agent-${VERSION}.pom
API_POM_URL=${BASE_PUBLIC_DOWNLOAD_URL}/newrelic-api/${VERSION}/newrelic-api-${VERSION}.pom

echo "Attempting to fetch artifacts from Public Facing Site"

#Download the newrelic zip directory
wget -O ${ZIP} ${AGENT_ZIP_URL}
if test $? -ne 0
  then
    echo "Error fetching ${ZIPNAME} from Public Site, aborting"
    exit 10
  fi

#Download the agent pom file
wget -O ${AGENT_POM} ${AGENT_POM_URL}
if test $? -ne 0
  then
    echo "Error fetching ${AGENT_POM_NAME} from Public Site, aborting"
    exit 10
  fi

#Download the api pom file
wget -O ${API_POM} ${API_POM_URL}
if test $? -ne 0
  then
    echo "Error fetching ${API_POM_NAME} from Public Site, aborting"
    exit 10
  fi

 echo "Successfully downloaded artifacts from Public Facing Site"

 /bin/sh "${INSTALLDIR}/send-zip-to-s3.sh" ${VERSION} ${ZIP} ${AGENT_POM} ${API_POM} ${SERVERAGENTDIR} ${SERVERAPIDIR} ${BASE_S3_URL} ${S3_PROFILE}

EXITSTATUS=$?

/bin/rm -rf ${MYDIR}

exit ${EXITSTATUS}
