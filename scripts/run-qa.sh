#!/usr/bin/env bash
set -euo pipefail
shopt -s nullglob
apks=(app/build/outputs/apk/qa/edoofox-*-qa.apk)
if (( ${#apks[@]} != 1 )); then
  echo "Expected exactly one QA APK; rebuild the QA variant." >&2
  exit 1
fi
adb install -r "${apks[0]}"
adb install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
mkdir -p artifacts/qa
for phase in gestures locales about; do
  # adb can return zero even when instrumentation crashes or reports FAIL.
  timeout 180s adb shell am instrument -w -e phase "$phase" \
    cz.weborama.edoofox.qa.test/cz.weborama.edoofox.SmokeInstrumentation \
    | tee "artifacts/qa/$phase.txt"
  if grep -Eq 'FAIL:|INSTRUMENTATION_FAILED|shortMsg=|Process crashed|INSTRUMENTATION_ABORTED' "artifacts/qa/$phase.txt"; then
    exit 1
  fi
  grep -Eq '(^|stream=)PASS:' "artifacts/qa/$phase.txt"
done
