#!/bin/bash
#
# Fails unless the PGP key in the SIGNING_KEY environment variable is listed in signing-fingerprints.txt.
# Run it before releasing, so a rotated key that hasn't been added to the list is caught before anything is uploaded
# to Maven Central and verification fails afterwards.
#
# Usage: SIGNING_KEY=<armored private key> check-signing-key.sh
#
# Environment overrides:
#   SIGNING_FINGERPRINTS_FILE  list of accepted keys (defaults to signing-fingerprints.txt next to this script)

SIGNING_FINGERPRINTS_FILE=${SIGNING_FINGERPRINTS_FILE:-$(dirname "$0")/signing-fingerprints.txt}

fail() {
  echo "SIGNING KEY CHECK FAILED: $1"
  exit 1
}

test -n "${SIGNING_KEY}" || fail "SIGNING_KEY is empty. Check that the SIGNING_KEY secret is available to this job."
command -v gpg > /dev/null || fail "gpg is not installed. Install it and re-run this script."
test -s "${SIGNING_FINGERPRINTS_FILE}" || fail "${SIGNING_FINGERPRINTS_FILE} is missing or empty."

GNUPGHOME=$(mktemp -d)
export GNUPGHOME
trap 'rm -rf "${GNUPGHOME}"' EXIT

# show-only lists the key without importing it, so no passphrase is needed. The first fpr line is the primary key.
FINGERPRINT=$(echo "${SIGNING_KEY}" | gpg --batch --with-colons --import-options show-only --import 2> /dev/null | awk -F: '/^fpr:/ { print $10; exit }')
test -n "${FINGERPRINT}" || fail "couldn't read a key from SIGNING_KEY. Check that the secret holds an ASCII-armored private key."

awk -v fingerprint="${FINGERPRINT}" '$1 == fingerprint { found = 1 } END { exit !found }' "${SIGNING_FINGERPRINTS_FILE}" \
  || fail "signing key ${FINGERPRINT} isn't in ${SIGNING_FINGERPRINTS_FILE}. Add it in a PR (generate-signing-key.sh prints the line), confirm the fingerprint with the key custodian, then re-run the release."

echo "Signing key ${FINGERPRINT} is listed in ${SIGNING_FINGERPRINTS_FILE}."
