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
# Requires an OAuth access token scoped to
# https://www.googleapis.com/auth/androidpublisher in $ACCESS_TOKEN — plain
# `gcloud auth print-access-token` mints one scoped to cloud-platform instead,
# which the Android Publisher API rejects, so the calling workflow must set
# `token_format: access_token` + `access_token_scopes` on
# google-github-actions/auth and pass its `access_token` output through
# rather than relying on the ADC credentials file. Also requires `curl`/`jq`
# on PATH.
#
# Usage: ACCESS_TOKEN=<token> ./get-next-version-code.sh <package-name>

set -euo pipefail

PACKAGE_NAME="${1:?usage: get-next-version-code.sh <package-name>}"
ACCESS_TOKEN="${ACCESS_TOKEN:?ACCESS_TOKEN env var must be set to an androidpublisher-scoped access token}"
API_BASE="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}"

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
