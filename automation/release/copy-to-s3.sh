#! /bin/bash


if test $# -ne 3
	then
		echo "Usage: $0 sonatype-staging-repo-name version-num s3-profile-name"
		exit 1
	fi

STAGING_REPO_NAME=$1
VERSION=$2
S3_PROFILE=$3

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

echo "Attempting to fetch ${ZIPNAME} from Sonatype staging"

MYDIR=/tmp/$$

MVNDIR=${MYDIR}/sonatype
POMPATH=${MVNDIR}/pom.xml
ZIP=${MVNDIR}/${ZIPNAME}
AGENT_POM=${MVNDIR}/${AGENT_POM_NAME}
API_POM=${MVNDIR}/${AGENT_API_NAME}
mkdir -p ${MVNDIR}
 /bin/cat > ${POMPATH} << EOPOM 
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.newrelic.test</groupId>
        <artifactId>download_artifacts</artifactId>
        <packaging>war</packaging>
        <version>1.0</version>
        
        <repositories>
          <repository>
            <id>sonatype-staging</id>
            <name>sonatype-release-staging</name>
            <url>https://oss.sonatype.org/service/local/repositories/${STAGING_REPO_NAME}/content</url>
          </repository>
          <repository>
            <id>sonatype-release</id>
            <name>sonatype-releases</name>
            <url>https://oss.sonatype.org/content/repositories/releases</url>
          </repository>

        </repositories>

        <properties>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        </properties>

        <dependencies>
                <dependency>
                        <groupId>com.newrelic.agent.java</groupId>
                        <artifactId>newrelic-java</artifactId>
                        <version>$VERSION</version>
                        <type>zip</type>
                </dependency>
                <dependency>
                        <groupId>com.newrelic.agent.java</groupId>
                        <artifactId>newrelic-agent</artifactId>
                        <version>$VERSION</version>
                        <type>pom</type>
                </dependency>
                <dependency>
                        <groupId>com.newrelic.agent.java</groupId>
                        <artifactId>newrelic-api</artifactId>
                        <version>$VERSION</version>
                        <type>pom</type>
                </dependency>
        </dependencies>

        <build>
        </build>
</project>
EOPOM
mvn -U -f ${POMPATH} clean dependency:copy-dependencies -DoutputDirectory=${MVNDIR}

if test $? -ne 0
	then
		echo Error fetching ${ZIPNAME} from Sonatype, aborting
		exit 10
	fi

echo "Fetch succeeded"

 /bin/sh "${INSTALLDIR}/send-zip-to-s3.sh" ${VERSION} ${ZIP} ${AGENT_POM} ${API_POM} ${SERVERAGENTDIR} ${SERVERAPIDIR} ${BASE_S3_URL} ${S3_PROFILE}

EXITSTATUS=$?

/bin/rm -rf ${MYDIR}

exit ${EXITSTATUS}
