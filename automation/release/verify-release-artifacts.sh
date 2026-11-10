#!/bin/bash
#
# Verifies release artifacts against the Sigstore-signed SHA256SUMS that publish_release.yml attaches to the GitHub release.
#
# Usage: verify-release-artifacts.sh [--complete] <release-tag> <sums-dir> <artifact-dir>
#   release-tag   the release tag, e.g. vX.X.X
#   sums-dir      directory containing SHA256SUMS and SHA256SUMS.sigstore.json
#   artifact-dir  directory (searched recursively) containing the .jar, .pom and .zip files to verify
#   --complete    every file listed in SHA256SUMS must be present, with a Sigstore bundle and a PGP signature
#
# Every .jar, .pom and .zip found in artifact-dir must be listed in SHA256SUMS with a matching hash and must have a
# build provenance attestation. Sigstore bundles (<file>.sigstore.json) and PGP signatures (<file>.asc) found next
# to an artifact are verified.
#
# Requires: sha256sum, sigstore (pipx install sigstore), gh (authenticated), gpg
#
# Environment overrides:
#   SIGNER_IDENTITY  expected Sigstore certificate identity (defaults to publish_release.yml at the release tag)
#   GITHUB_REPOSITORY  repository the release and attestations belong to (defaults to newrelic/newrelic-java-agent)

# Fingerprint of the "New Relic Build Server" key that signs the .asc files published to Maven Central
PGP_SIGNING_FINGERPRINT=8C2A4092C45DCCA3FB7C64CE61AB23A88CC1C2D5
PGP_KEYSERVER=hkps://keyserver.ubuntu.com
SIGNER_OIDC_ISSUER=https://token.actions.githubusercontent.com

COMPLETE=false
if [ "$1" = "--complete" ]; then
  COMPLETE=true
  shift
fi

if test $# -ne 3
  then
    echo "Usage: $0 [--complete] release-tag sums-dir artifact-dir"
    exit 1
  fi

RELEASE_TAG=$1
SUMS_DIR=$2
ARTIFACT_DIR=$3
REPOSITORY=${GITHUB_REPOSITORY:-newrelic/newrelic-java-agent}
SIGNER_IDENTITY=${SIGNER_IDENTITY:-https://github.com/${REPOSITORY}/.github/workflows/publish_release.yml@refs/tags/${RELEASE_TAG}}
SUMS=${SUMS_DIR}/SHA256SUMS

fail() {
  echo "VERIFICATION FAILED: $1"
  exit 2
}

for TOOL in sha256sum sigstore gh gpg; do
  command -v ${TOOL} > /dev/null || fail "${TOOL} is not installed. Install it and re-run this script."
done

test -s "${SUMS}" || fail "${SUMS} is missing or empty. Download it from the ${RELEASE_TAG} GitHub release."
test -s "${SUMS}.sigstore.json" || fail "${SUMS}.sigstore.json is missing. Download it from the ${RELEASE_TAG} GitHub release."

echo "Verifying ${SUMS} was signed by ${SIGNER_IDENTITY}"
sigstore verify identity --cert-identity "${SIGNER_IDENTITY}" --cert-oidc-issuer "${SIGNER_OIDC_ISSUER}" "${SUMS}" \
  || fail "the Sigstore signature on SHA256SUMS is invalid or wasn't created by ${SIGNER_IDENTITY}."

ARTIFACTS=()
while IFS= read -r -d '' FILE; do
  ARTIFACTS+=("${FILE}")
done < <(find "${ARTIFACT_DIR}" -type f \( -name '*.jar' -o -name '*.pom' -o -name '*.zip' \) -print0 | sort -z)

test ${#ARTIFACTS[@]} -gt 0 || fail "no .jar, .pom or .zip files found in ${ARTIFACT_DIR}."

echo "Checking ${#ARTIFACTS[@]} artifacts against SHA256SUMS"
BUNDLED=()
GNUPGHOME=$(mktemp -d)
export GNUPGHOME
trap 'rm -rf "${GNUPGHOME}"' EXIT
PGP_KEY_IMPORTED=false

for FILE in "${ARTIFACTS[@]}"; do
  NAME=$(basename "${FILE}")
  EXPECTED=$(awk -v name="${NAME}" '$2 == name { print $1 }' "${SUMS}")
  test -n "${EXPECTED}" || fail "${NAME} isn't listed in SHA256SUMS, so it wasn't produced by the ${RELEASE_TAG} release."
  ACTUAL=$(sha256sum "${FILE}" | awk '{ print $1 }')
  test "${EXPECTED}" = "${ACTUAL}" || fail "${NAME} has SHA-256 ${ACTUAL}, but SHA256SUMS lists ${EXPECTED}."

  if [ -f "${FILE}.sigstore.json" ]; then
    BUNDLED+=("${FILE}")
  elif [ "${COMPLETE}" = "true" ]; then
    fail "${NAME}.sigstore.json is missing."
  fi

  if [ -f "${FILE}.asc" ]; then
    if [ "${PGP_KEY_IMPORTED}" = "false" ]; then
      gpg --batch --quiet --keyserver "${PGP_KEYSERVER}" --recv-keys "${PGP_SIGNING_FINGERPRINT}" \
        || fail "couldn't fetch PGP key ${PGP_SIGNING_FINGERPRINT} from ${PGP_KEYSERVER}. Check network access to the keyserver."
      PGP_KEY_IMPORTED=true
    fi
    gpg --batch --status-fd 1 --verify "${FILE}.asc" "${FILE}" 2> /dev/null | grep -q "^\[GNUPG:\] VALIDSIG .* ${PGP_SIGNING_FINGERPRINT}$" \
      || fail "${NAME}.asc isn't a valid signature from PGP key ${PGP_SIGNING_FINGERPRINT}."
  elif [ "${COMPLETE}" = "true" ]; then
    fail "${NAME}.asc is missing."
  fi

  gh attestation verify "${FILE}" --repo "${REPOSITORY}" --cert-identity "${SIGNER_IDENTITY}" --cert-oidc-issuer "${SIGNER_OIDC_ISSUER}" > /dev/null \
    || fail "${NAME} has no valid build provenance attestation from ${SIGNER_IDENTITY}."
done

if [ ${#BUNDLED[@]} -gt 0 ]; then
  echo "Verifying ${#BUNDLED[@]} Sigstore bundles"
  sigstore verify identity --cert-identity "${SIGNER_IDENTITY}" --cert-oidc-issuer "${SIGNER_OIDC_ISSUER}" "${BUNDLED[@]}" \
    || fail "one or more Sigstore bundles are invalid or weren't created by ${SIGNER_IDENTITY}."
fi

if [ "${COMPLETE}" = "true" ]; then
  while read -r _ NAME; do
    FOUND=false
    for FILE in "${ARTIFACTS[@]}"; do
      if [ "$(basename "${FILE}")" = "${NAME}" ]; then
        FOUND=true
        break
      fi
    done
    test "${FOUND}" = "true" || fail "${NAME} is listed in SHA256SUMS but wasn't found in ${ARTIFACT_DIR}."
  done < "${SUMS}"
fi

echo "Verified ${#ARTIFACTS[@]} artifacts for ${RELEASE_TAG}."
