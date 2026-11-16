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

#Download the PGP signatures of the zip and pom files
for URL in ${AGENT_ZIP_URL} ${AGENT_POM_URL} ${API_POM_URL}
  do
    wget -O ${MVNDIR}/`basename ${URL}`.asc ${URL}.asc
    if test $? -ne 0
      then
        echo "Error fetching `basename ${URL}`.asc from Public Site, aborting"
        exit 10
      fi
  done

 echo "Successfully downloaded artifacts from Public Facing Site"

#Verify the downloads against the signed SHA256SUMS on the GitHub release before anything is copied to S3
gh release download "v${VERSION}" --repo "${GITHUB_REPOSITORY:-newrelic/newrelic-java-agent}" --pattern 'SHA256SUMS*' --dir ${MVNDIR}
if test $? -ne 0
  then
    echo "Error fetching SHA256SUMS from the v${VERSION} GitHub release, aborting. Releases published before artifact signing was added don't have one."
    exit 11
  fi

/bin/bash "${INSTALLDIR}/verify-release-artifacts.sh" "v${VERSION}" ${MVNDIR} ${MVNDIR}
if test $? -ne 0
  then
    echo "Artifacts downloaded for ${VERSION} failed verification, aborting. Nothing was copied to S3."
    /bin/rm -rf ${MYDIR}
    exit 12
  fi

 /bin/sh "${INSTALLDIR}/send-zip-to-s3.sh" ${VERSION} ${ZIP} ${AGENT_POM} ${API_POM} ${SERVERAGENTDIR} ${SERVERAPIDIR} ${BASE_S3_URL} ${S3_PROFILE} ${MVNDIR}

EXITSTATUS=$?

# Download what landed in S3 and verify it against the signed SHA256SUMS, to catch partial or corrupted uploads
if test ${EXITSTATUS} -eq 0
  then
    if [ -z "${RUNNING_ON_GHA}" ]
      then
        PROFILE="--profile ${S3_PROFILE}"
      fi
    S3CHECKDIR=${MYDIR}/s3-check
    mkdir -p ${S3CHECKDIR}/newrelic-agent ${S3CHECKDIR}/newrelic-api
    S3_DOWNLOAD_OK=true
    for FILE in SHA256SUMS SHA256SUMS.sigstore.json newrelic-java-${VERSION}.zip newrelic-agent-${VERSION}.jar newrelic-agent-${VERSION}.pom
      do
        DEST=${S3CHECKDIR}/newrelic-agent
        test ${FILE} = SHA256SUMS -o ${FILE} = SHA256SUMS.sigstore.json && DEST=${S3CHECKDIR}
        aws s3 ${PROFILE} cp s3://${SERVERAGENTDIR}/${FILE} ${DEST}/${FILE} || S3_DOWNLOAD_OK=false
      done
    for FILE in newrelic-java-${VERSION}.zip newrelic-api-${VERSION}.jar newrelic-api-${VERSION}.pom
      do
        aws s3 ${PROFILE} cp s3://${SERVERAPIDIR}/${FILE} ${S3CHECKDIR}/newrelic-api/${FILE} || S3_DOWNLOAD_OK=false
      done
    if test ${S3_DOWNLOAD_OK} = false
      then
        echo "Could not download the uploaded artifacts for ${VERSION} from S3 to verify them. Check the S3 listing and re-run the copy."
        EXITSTATUS=13
      else
        /bin/bash "${INSTALLDIR}/verify-release-artifacts.sh" "v${VERSION}" ${S3CHECKDIR} ${S3CHECKDIR}
        if test $? -ne 0
          then
            echo "Artifacts uploaded to S3 for ${VERSION} failed verification. Investigate and re-run the copy before announcing the release."
            EXITSTATUS=14
          fi
      fi
  fi

/bin/rm -rf ${MYDIR}

exit ${EXITSTATUS}
