#!/bin/sh

if test $# -ne 8
	then
		echo "Usage: $0 version path-to-zip path-to-agent-pom path-to-api-pom target-agent-dir target-api-dir base-s3-url s3-profile-name"
		exit 1
	fi

VERSION=$1
ZIP=$2
AGENT_POM=$3
API_POM=$4
TARGET_AGENT_DIR=$5
TARGET_API_DIR=$6
BASE_S3_URL=$7
S3_PROFILE=$8

CURRENT_AGENT_DIR=${BASE_S3_URL}/newrelic-agent/current
CURRENT_API_DIR=${BASE_S3_URL}/newrelic-api/current


if test \! -f ${ZIP}
    then
        echo "$ZIP is not a file"
        exit 5
    fi

if test \! -f ${AGENT_POM}
    then
        echo "$AGENT_POM is not a file"
        exit 5
    fi

if test \! -f ${API_POM}
    then
        echo "$API_POM is not a file"
        exit 5
    fi


MYDIR=/tmp/$$

TEMPDIR=${MYDIR}/${VERSION}
echo temp directory is ${TEMPDIR}

mkdir -p ${TEMPDIR}

mkdir -p ${TEMPDIR}/newrelic-agent/${VERSION}
mkdir -p ${TEMPDIR}/newrelic-api/${VERSION}

cp ${ZIP} ${TEMPDIR}/newrelic-agent/${VERSION}
unzip -j ${ZIP} newrelic/newrelic.yml -d ${TEMPDIR}/newrelic-agent/${VERSION}
unzip -j ${ZIP} newrelic/newrelic.jar -d ${TEMPDIR}/newrelic-agent/${VERSION}
mv ${TEMPDIR}/newrelic-agent/${VERSION}/newrelic.jar ${TEMPDIR}/newrelic-agent/${VERSION}/newrelic-agent-${VERSION}.jar
cp ${AGENT_POM} ${TEMPDIR}/newrelic-agent/${VERSION}/newrelic-agent-${VERSION}.pom

cp ${ZIP} ${TEMPDIR}/newrelic-api/${VERSION}
unzip -j ${ZIP} newrelic/newrelic-api.jar -d ${TEMPDIR}/newrelic-api/${VERSION}
mv ${TEMPDIR}/newrelic-api/${VERSION}/newrelic-api.jar ${TEMPDIR}/newrelic-api/${VERSION}/newrelic-api-${VERSION}.jar
cp ${API_POM} ${TEMPDIR}/newrelic-api/${VERSION}/newrelic-api-${VERSION}.pom


EXITSTATUS=0



# Sync $TEMPDIR to s3
aws s3 --profile ${S3_PROFILE} sync ${TEMPDIR}/newrelic-agent/${VERSION} s3://${TARGET_AGENT_DIR}
aws s3 --profile ${S3_PROFILE} sync ${TEMPDIR}/newrelic-api/${VERSION} s3://${TARGET_API_DIR}

# Add unversioned zip file 
aws s3 --profile ${S3_PROFILE} cp ${TEMPDIR}/newrelic-agent/${VERSION}/newrelic-java-${VERSION}.zip s3://${TARGET_AGENT_DIR}/newrelic-java.zip
aws s3 --profile ${S3_PROFILE} cp ${TEMPDIR}/newrelic-api/${VERSION}/newrelic-java-${VERSION}.zip s3://${TARGET_API_DIR}/newrelic-java.zip

# This is to compare/sync this release version to s3://{BASE_S3_URL}/newrelic-agent/current.
# Otherwise, 2.21.x and possibly other maintenance branches will override it
CURRENT=$(aws s3 --profile $S3_PROFILE ls s3://$BASE_S3_URL/newrelic-agent/ |
                 grep PRE |
                 awk '{print $2}' |
                 sort -t '.' -k 1,1 -k 2,2 -k 3,3 -k 4,4 -g |
                 sed 's/\///g' |
                 tail -1)

if [ ${CURRENT} = ${VERSION} ]
then
    aws s3 --profile ${S3_PROFILE} sync ${TEMPDIR}/newrelic-agent/${VERSION} s3://${CURRENT_AGENT_DIR} --delete
    aws s3 --profile ${S3_PROFILE} sync ${TEMPDIR}/newrelic-api/${VERSION} s3://${CURRENT_API_DIR} --delete

   # Some customers depend on unversioned jar files in the current directory for automation. See https://newrelic.atlassian.net/browse/JAVA-2715
   aws s3 --profile ${S3_PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-agent-${VERSION}.jar s3://${CURRENT_AGENT_DIR}/newrelic-agent.jar
   aws s3 --profile ${S3_PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-agent-${VERSION}.jar s3://${CURRENT_AGENT_DIR}/newrelic.jar
   aws s3 --profile ${S3_PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-agent-${VERSION}.pom s3://${CURRENT_AGENT_DIR}/newrelic-agent.pom
   aws s3 --profile ${S3_PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-java-${VERSION}.zip s3://${CURRENT_AGENT_DIR}/newrelic-java.zip

   aws s3 --profile ${S3_PROFILE} cp s3://${CURRENT_API_DIR}/newrelic-api-${VERSION}.jar s3://${CURRENT_API_DIR}/newrelic-api.jar
   aws s3 --profile ${S3_PROFILE} cp s3://${CURRENT_API_DIR}/newrelic-api-${VERSION}.pom s3://${CURRENT_API_DIR}/newrelic-api.pom
   aws s3 --profile ${S3_PROFILE} cp s3://${CURRENT_API_DIR}/newrelic-java-${VERSION}.zip s3://${CURRENT_API_DIR}/newrelic-java.zip
else
    echo "This shouldn't happen!"
    echo "Is this a 2.21.x Release?"
fi

exit ${EXITSTATUS}
