#!/bin/bash
#
# Downloads every artifact listed in a release's SHA256SUMS from Maven Central, with its Sigstore bundle and PGP
# signature, so it can be checked with verify-release-artifacts.sh.
#
# Usage: download-central-release.sh <release-tag> <sums-dir> <dest-dir>
#   release-tag  the release tag, e.g. vX.X.X
#   sums-dir     directory containing SHA256SUMS from the GitHub release
#   dest-dir     directory the artifacts are downloaded to

if test $# -ne 3
  then
    echo "Usage: $0 release-tag sums-dir dest-dir"
    exit 1
  fi

RELEASE_TAG=$1
SUMS=$2/SHA256SUMS
DEST_DIR=$3
VERSION=${RELEASE_TAG#v}
BASE_URL=https://repo1.maven.org/maven2/com/newrelic/agent/java

test -s "${SUMS}" || { echo "${SUMS} is missing or empty. Download it from the ${RELEASE_TAG} GitHub release."; exit 1; }
mkdir -p "${DEST_DIR}"

while read -r _ NAME; do
  # Artifact file names are <artifactId>-<version>[-<classifier>].<extension>. Some artifacts, such as the
  # OpenTelemetry extension, publish with a "-alpha" version suffix.
  ARTIFACT_ID=${NAME%%-${VERSION}*}
  if [ "${ARTIFACT_ID}" = "${NAME}" ]; then
    echo "Can't determine the artifactId of ${NAME} for version ${VERSION}. Check that ${RELEASE_TAG} is the right release tag."
    exit 1
  fi
  ARTIFACT_VERSION=${VERSION}
  case "${NAME}" in
    "${ARTIFACT_ID}-${VERSION}-alpha"*) ARTIFACT_VERSION=${VERSION}-alpha ;;
  esac

  for FILE in "${NAME}" "${NAME}.sigstore.json" "${NAME}.asc"; do
    URL=${BASE_URL}/${ARTIFACT_ID}/${ARTIFACT_VERSION}/${FILE}
    curl -sfL --retry 3 -o "${DEST_DIR}/${FILE}" "${URL}" \
      || { echo "Failed to download ${URL}. Check that ${RELEASE_TAG} has been published to Maven Central and synced."; exit 1; }
  done
done < "${SUMS}"

echo "Downloaded $(wc -l < "${SUMS}" | tr -d ' ') artifacts for ${RELEASE_TAG} to ${DEST_DIR}."
