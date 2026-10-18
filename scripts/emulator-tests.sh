#!/usr/bin/env bash
set -uo pipefail
adb shell settings put global hide_error_dialogs 1
adb shell am force-stop com.android.launcher3
gradle --no-daemon -PtestAbi=x86_64 :app:connectedDebugAndroidTest
result=$?
adb pull /data/local/tmp/linkgrab-screenshots screenshots || result=1
adb logcat -d -v brief AndroidRuntime:E '*:S' > crash.log || true
exit "$result"
