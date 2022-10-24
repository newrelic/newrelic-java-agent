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

# when running on GHA, we cannot specify the profile, like we do on our managed machines.
# Auth is done thru the aws-actions/configure-aws-credentials action.
if [ -z ${RUNNING_ON_GHA} ];
then
  echo "Running locally"
  PROFILE="--profile ${S3_PROFILE}"
  OUTPUT=/dev/stdout
else
  echo "Running on Gha"
  OUTPUT=$GITHUB_STEP_SUMMARY
fi

# Sync agent's $TEMPDIR to s3
aws s3 ${PROFILE} sync ${TEMPDIR}/newrelic-agent/${VERSION} s3://${TARGET_AGENT_DIR}
if [ $? ]
then
  echo ":white_check_mark: Agent uploaded successfully." >> $OUTPUT
else
  echo ":x: Failed to upload agent." >> $OUTPUT
fi

# Sync api's $TEMPDIR to s3
aws s3 ${PROFILE} sync ${TEMPDIR}/newrelic-api/${VERSION} s3://${TARGET_API_DIR}
if [ $? ]
then
  echo ":white_check_mark: API uploaded successfully." >> $OUTPUT
else
  echo ":x: Failed to upload API." >> $OUTPUT
fi

# Add unversioned agent zip file
aws s3 ${PROFILE} cp ${TEMPDIR}/newrelic-agent/${VERSION}/newrelic-java-${VERSION}.zip s3://${TARGET_AGENT_DIR}/newrelic-java.zip
if [ $? ]
then
  echo ":white_check_mark: Unversioned agent uploaded successfully." >> $OUTPUT
else
  echo ":x: Failed to upload unversioned agent." >> $OUTPUT
fi

# Add unversioned api zip file
aws s3 ${PROFILE} cp ${TEMPDIR}/newrelic-api/${VERSION}/newrelic-java-${VERSION}.zip s3://${TARGET_API_DIR}/newrelic-java.zip
if [ $? ]
then
  echo ":white_check_mark: Unversioned API uploaded successfully." >> $OUTPUT
else
  echo ":x: Failed to upload unversioned API." >> $OUTPUT
fi

# This is to compare/sync this release version to s3://{BASE_S3_URL}/newrelic-agent/current.
# Otherwise, 2.21.x and possibly other maintenance branches will override it
CURRENT=$(aws s3 ${PROFILE} ls s3://$BASE_S3_URL/newrelic-agent/ |
                 grep PRE |
                 awk '{print $2}' |
                 sort -t '.' -k 1,1 -k 2,2 -k 3,3 -k 4,4 -g |
                 sed 's/\///g' |
                 tail -1)

if [ ${CURRENT} = ${VERSION} ];
then
    aws s3 ${PROFILE} sync ${TEMPDIR}/newrelic-agent/${VERSION} s3://${CURRENT_AGENT_DIR} --delete
    aws s3 ${PROFILE} sync ${TEMPDIR}/newrelic-api/${VERSION} s3://${CURRENT_API_DIR} --delete

   # Some customers depend on unversioned jar files in the current directory for automation. See https://newrelic.atlassian.net/browse/JAVA-2715
   aws s3 ${PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-agent-${VERSION}.jar s3://${CURRENT_AGENT_DIR}/newrelic-agent.jar
   aws s3 ${PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-agent-${VERSION}.jar s3://${CURRENT_AGENT_DIR}/newrelic.jar
   aws s3 ${PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-agent-${VERSION}.pom s3://${CURRENT_AGENT_DIR}/newrelic-agent.pom
   aws s3 ${PROFILE} cp s3://${CURRENT_AGENT_DIR}/newrelic-java-${VERSION}.zip s3://${CURRENT_AGENT_DIR}/newrelic-java.zip

   aws s3 ${PROFILE} cp s3://${CURRENT_API_DIR}/newrelic-api-${VERSION}.jar s3://${CURRENT_API_DIR}/newrelic-api.jar
   aws s3 ${PROFILE} cp s3://${CURRENT_API_DIR}/newrelic-api-${VERSION}.pom s3://${CURRENT_API_DIR}/newrelic-api.pom
   aws s3 ${PROFILE} cp s3://${CURRENT_API_DIR}/newrelic-java-${VERSION}.zip s3://${CURRENT_API_DIR}/newrelic-java.zip
   echo ":white_check_mark: Current directory updated." >> $OUTPUT
else
   echo ":x: Looks like you uploaded an update to an older version, so the current directory was not updated." >> $OUTPUT
   echo ":x: If this release was supposed to update the current directory, something went wrong." >> $OUTPUT
fi

exit ${EXITSTATUS}
