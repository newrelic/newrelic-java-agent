#!/usr/bin/env bash
#
# Generates a new GPG key pair for Sonatype/Maven Central artifact signing,
# prints the values needed for the SIGNING_KEY_ID secret and uploads the
# key to the hkps://keyserver.ubuntu.com keyserver.


set -euo pipefail

REAL_NAME="newrelic-java-agent-dev"
EMAIL="java-agent-dev@newrelic.com"

print_help() {
  cat <<HELP
Generate a new GPG signing key for Sonatype/Maven Central publishing.

Usage:
  $0 [-u|--upload] <passphrase> [expiration] [output-file] [keyserver]

Arguments:
  passphrase    Required. Becomes SIGNING_PASSWORD. Must be at least 20
                characters long and contain at least one digit and one
                special (non-alphanumeric) character.
  expiration    Optional. gpg expiration spec, e.g. 1y, 2y, 18m, 6m.
                Default: 2y
  output-file   Optional. Path to write the exported private key to.
                Default: ./private-key.asc
                Refuses to run if this file already exists.
  keyserver     Optional. Keyserver to publish the public key to.
                Default: hkps://keyserver.ubuntu.com
                Only used if -u/--upload is also given.

Options:
  -u, --upload  Upload the public key to the keyserver during this run.
                Can appear anywhere on the command line. Without this
                flag, no keyserver upload is attempted at all.
  -h, --help    Show this help message and exit.

Identity used for the generated key (fixed for this project):
  Real name: ${REAL_NAME}
  Email:     ${EMAIL}

Note: the key is generated in an isolated scratch keyring that is deleted
when the script exits. If you skip -u/--upload here, the key no longer
exists in any keyring afterward - only in the exported file - so a later
upload requires re-importing that file first:
    export GNUPGHOME=\$(mktemp -d)
    gpg --batch --import <output-file>
    gpg --keyserver <keyserver> --send-keys <fingerprint>
    rm -rf "\$GNUPGHOME"

Examples:
  $0 --upload 'This-is-my-long-password!-99!'
  $0 'This-is-my-long-password!-99!' 18m /tmp/new-signing-key.asc
HELP
}

UPLOAD=0
ARGS=()
for arg in "$@"; do
  case "$arg" in
    -h|--help)
      print_help
      exit 0
      ;;
    -u|--upload)
      UPLOAD=1
      ;;
    *)
      ARGS+=("$arg")
      ;;
  esac
done

PASSPHRASE="${ARGS[0]:-}"
EXPIRATION="${ARGS[1]:-2y}"
OUTPUT_FILE="${ARGS[2]:-./private-key.asc}"
KEYSERVER="${ARGS[3]:-hkps://keyserver.ubuntu.com}"

if [[ -z "$PASSPHRASE" ]]; then
  echo "Error: passphrase is required." >&2
  echo >&2
  print_help >&2
  exit 1
fi

if [[ ${#PASSPHRASE} -lt 20 ]]; then
  echo "Error: passphrase must be at least 20 characters." >&2
  exit 1
fi

if ! [[ "$PASSPHRASE" =~ [0-9] ]]; then
  echo "Error: passphrase must contain at least one digit." >&2
  exit 1
fi

if ! [[ "$PASSPHRASE" =~ [^a-zA-Z0-9] ]]; then
  echo "Error: passphrase must contain at least one special character." >&2
  exit 1
fi

if [[ -e "$OUTPUT_FILE" ]]; then
  echo "Error: output file '$OUTPUT_FILE' already exists. Refusing to overwrite." >&2
  exit 1
fi

export GNUPGHOME="$(mktemp -d)"
chmod 700 "$GNUPGHOME"
echo "allow-loopback-pinentry" > "$GNUPGHOME/gpg-agent.conf"
gpgconf --reload gpg-agent >/dev/null 2>&1 || true

cleanup() {
  gpgconf --homedir "$GNUPGHOME" --kill gpg-agent >/dev/null 2>&1 || true
  rm -rf "$GNUPGHOME"
}
trap cleanup EXIT

PARAM_FILE="$(mktemp)"
chmod 600 "$PARAM_FILE"
cat > "$PARAM_FILE" <<EOF
Key-Type: RSA
Key-Length: 4096
Key-Usage: sign,cert
Subkey-Type: RSA
Subkey-Length: 4096
Subkey-Usage: encrypt
Name-Real: ${REAL_NAME}
Name-Email: ${EMAIL}
Expire-Date: ${EXPIRATION}
Passphrase: ${PASSPHRASE}
%commit
EOF

echo "Generating new 4096-bit RSA key pair for ${REAL_NAME} <${EMAIL}> (expires: ${EXPIRATION})..."
gpg --batch --pinentry-mode loopback --gen-key "$PARAM_FILE"
rm -P "$PARAM_FILE"

FPR="$(gpg --batch --list-secret-keys --with-colons --fixed-list-mode "$EMAIL" | awk -F: '/^fpr:/ {print $10; exit}')"
LONG_ID="$(gpg --batch --list-secret-keys --with-colons --fixed-list-mode "$EMAIL" | awk -F: '/^sec:/ {print $5; exit}')"
SHORT_ID="${LONG_ID: -8}"

gpg --batch --pinentry-mode loopback --passphrase "$PASSPHRASE" --armor --export-secret-keys "$FPR" > "$OUTPUT_FILE"
chmod 600 "$OUTPUT_FILE"

UPLOAD_OK=1
if [[ "$UPLOAD" -eq 1 ]]; then
  echo "Uploading public key to ${KEYSERVER}..."
  gpg --batch --keyserver "${KEYSERVER}" --send-keys "$FPR" || UPLOAD_OK=0
fi

echo
echo "Key generated successfully."
echo "  SIGNING_KEY_ID (short form - this is what Gradle's signing plugin needs):"
echo "    ${SHORT_ID}"
echo "  Full fingerprint:"
echo "    ${FPR}"
echo "  Private key exported to: ${OUTPUT_FILE}"
echo
if [[ "$UPLOAD" -eq 0 ]]; then
  echo "Public key NOT uploaded (pass -u/--upload to do this automatically)."
  echo "To upload it later, re-import the exported file first:"
  echo "    export GNUPGHOME=\$(mktemp -d)"
  echo "    gpg --batch --import ${OUTPUT_FILE}"
  echo "    gpg --keyserver ${KEYSERVER} --send-keys ${FPR}"
  echo "    rm -rf \"\$GNUPGHOME\""
elif [[ "$UPLOAD_OK" -eq 1 ]]; then
  echo "Public key uploaded to ${KEYSERVER}."
else
  echo "WARNING: public key upload to ${KEYSERVER} failed. The scratch keyring" >&2
  echo "is about to be deleted, so retry via a fresh import of ${OUTPUT_FILE}:" >&2
  echo "    export GNUPGHOME=\$(mktemp -d)" >&2
  echo "    gpg --batch --import ${OUTPUT_FILE}" >&2
  echo "    gpg --keyserver ${KEYSERVER} --send-keys ${FPR}" >&2
  echo "    rm -rf \"\$GNUPGHOME\"" >&2
fi
echo
echo "Next steps:"
echo "  1. Attach ${OUTPUT_FILE} to the 1Password 'Sonatype signing key' entry"
echo "     as a FILE ATTACHMENT, not pasted into a text field."
echo "  2. Update SIGNING_KEY_ID and SIGNING_PASSWORD in the same entry."
echo "  3. Delete the local file once it's safely stored:"
echo "       rm -P ${OUTPUT_FILE}"
