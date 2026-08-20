#!/usr/bin/env bash
#
# Prints the next Android versionCode for com.willowtree.vocable, computed as
# (highest versionCode already published to any Play Store track) + 1.
#
# This exists because both release workflows used to derive versionCode from
# `github.run_number`, which is scoped per-workflow-file — a workflow that has
# never run (or runs rarely) starts its counter at 1, independent of how far
# along a more frequently-run sibling workflow's counter is. That let a
# lower/duplicate versionCode reach Play Store, which rejects any upload with
# a versionCode <= one already used on any track. Querying Play directly for
# ground truth removes the dependency on run history entirely.
#
# Requires the Play service account's JSON key in $SERVICE_ACCOUNT_JSON (the
# same key used by r0adkll/upload-google-play). We mint our own
# androidpublisher-scoped access token from it via the standard OAuth2
# JWT-bearer flow (google-auth's Credentials.refresh()), the same mechanism
# r0adkll's action uses internally. This deliberately avoids
# google-github-actions/auth's `token_format: access_token` output: that path
# routes through the IAM Service Account Credentials API
# (iamcredentials.googleapis.com), which is disabled on this GCP project and
# would need to be enabled in the Google Cloud Console first — an infra
# change, not a code fix. `gcloud auth print-access-token` was tried too and
# doesn't work either: it has no --scopes flag, so it always mints a
# cloud-platform-scoped token, which the Android Publisher API rejects.
#
# Also requires python3 (with the `google-auth` package installed by the
# calling workflow) and `curl`/`jq` on PATH.
#
# Usage: SERVICE_ACCOUNT_JSON=<json> ./get-next-version-code.sh <package-name>

set -euo pipefail

PACKAGE_NAME="${1:?usage: get-next-version-code.sh <package-name>}"
: "${SERVICE_ACCOUNT_JSON:?SERVICE_ACCOUNT_JSON env var must be set to the Play service account key JSON}"
API_BASE="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}"

ACCESS_TOKEN=$(SERVICE_ACCOUNT_JSON="${SERVICE_ACCOUNT_JSON}" python3 - <<'PY'
import json
import os

from google.auth.transport.requests import Request
from google.oauth2 import service_account

info = json.loads(os.environ["SERVICE_ACCOUNT_JSON"])
credentials = service_account.Credentials.from_service_account_info(
    info, scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
credentials.refresh(Request())
print(credentials.token)
PY
)

if [[ -z "${ACCESS_TOKEN}" || "${ACCESS_TOKEN}" == "None" ]]; then
  echo "Failed to mint an androidpublisher-scoped access token" >&2
  exit 1
fi

EDIT_RESPONSE=$(curl -sS -X POST \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  "${API_BASE}/edits")
EDIT_ID=$(echo "${EDIT_RESPONSE}" | jq -r '.id')

if [[ -z "${EDIT_ID}" || "${EDIT_ID}" == "null" ]]; then
  echo "Failed to create a Play Console edit for ${PACKAGE_NAME}. Response:" >&2
  echo "${EDIT_RESPONSE}" >&2
  exit 1
fi

cleanup() {
  curl -sS -X DELETE \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${API_BASE}/edits/${EDIT_ID}" >/dev/null || true
}
trap cleanup EXIT

TRACKS_JSON=$(curl -sS \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  "${API_BASE}/edits/${EDIT_ID}/tracks")

MAX_VERSION_CODE=$(echo "${TRACKS_JSON}" | jq '[.tracks[]?.releases[]?.versionCodes[]? | tonumber] | max // 0')

echo $((MAX_VERSION_CODE + 1))
