#! /bin/bash

if test $# -ne 5
	then
		echo "Usage: $0 sonatype-deployment-id version-num s3-profile-name sonatype-username sonatype-password"
		exit 1
	fi

DEPLOYMENT_ID=$1
VERSION=$2
S3_PROFILE=$3
SONATYPE_USERNAME=$4
SONATYPE_PASSWORD=$5

BASE_S3_URL=java-agent-release-staging
if [ "$S3_PROFILE" = "default" ]; then
    BASE_S3_URL=nr-downloads-main/newrelic/java-agent
fi

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

SONATYPE_TOKEN=$(printf "$SONATYPE_USERNAME:$SONATYPE_PASSWORD" | base64)
BASE_SONATYPE_URL=https://central.sonatype.com/api/v1/publisher/deployment/${DEPLOYMENT_ID}/download/com/newrelic/agent/java
AGENT_ZIP_URL=${BASE_SONATYPE_URL}/newrelic-java/${VERSION}/newrelic-java-${VERSION}.zip
AGENT_POM_URL=${BASE_SONATYPE_URL}/newrelic-agent/${VERSION}/newrelic-agent-${VERSION}.pom
API_POM_URL=${BASE_SONATYPE_URL}/newrelic-api/${VERSION}/newrelic-api-${VERSION}.pom

echo "Attempting to fetch artifacts from Central Sonatype"

#Download the newrelic zip directory
wget --header="Authorization: Bearer $SONATYPE_TOKEN" -O ${ZIP} ${AGENT_ZIP_URL}
if test $? -ne 0
  then
    echo "Error fetching ${ZIPNAME} from Sonatype, aborting"
    exit 10
  fi

#Download the agent pom file
wget --header="Authorization: Bearer $SONATYPE_TOKEN" -O ${AGENT_POM} ${AGENT_POM_URL}
if test $? -ne 0
  then
    echo "Error fetching ${AGENT_POM_NAME} from Sonatype, aborting"
    exit 10
  fi

#Download the api pom file
wget --header="Authorization: Bearer $SONATYPE_TOKEN" -O ${API_POM} ${API_POM_URL}
if test $? -ne 0
  then
    echo "Error fetching ${API_POM_NAME} from Sonatype, aborting"
    exit 10
  fi

 echo "Successfully downloaded artifacts from Central Sonatype"

 /bin/sh "${INSTALLDIR}/send-zip-to-s3.sh" ${VERSION} ${ZIP} ${AGENT_POM} ${API_POM} ${SERVERAGENTDIR} ${SERVERAPIDIR} ${BASE_S3_URL} ${S3_PROFILE}

EXITSTATUS=$?

/bin/rm -rf ${MYDIR}

exit ${EXITSTATUS}
