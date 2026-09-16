#!/usr/bin/env bash
set -euo pipefail
platform="${1:-}"
coverage="${2:-smoke}"
reporting="zebrunner"
device=""
arguments=()
if [ "$#" -gt 2 ]; then
    arguments=("${@:3}")
fi
while [ "${#arguments[@]}" -gt 0 ]; do
    case "${arguments[0]}" in
        local|zebrunner)
            reporting="${arguments[0]}"
            arguments=("${arguments[@]:1}")
            ;;
        --device)
            if [ "${#arguments[@]}" -lt 2 ]; then
                printf 'Provide a device after --device.\n' >&2
                exit 2
            fi
            device="${arguments[1]}"
            arguments=("${arguments[@]:2}")
            ;;
        *)
            printf 'Usage: bash run-tests.sh android|ios [smoke|regression|grid|filter|cart|account|login] [local|zebrunner] [--device android-1|android-2|ios-1|ios-2]\n' >&2
            exit 2
            ;;
    esac
done
case "$coverage" in
    smoke|regression|grid|filter|cart|account|login) ;;
    *) printf 'Choose smoke, regression, grid, filter, cart, account, or login.\n' >&2; exit 2 ;;
esac
case "$platform" in
    android)
        suite="$coverage"
        device="${device:-android-1}"
        case "$device" in
            android-1)
                device_args=(-Dcapabilities.udid=emulator-5554 -Dcapabilities.deviceName=emulator-5554 -Dcapabilities.systemPort=8200 -Dcapabilities.mjpegServerPort=7810)
                ;;
            android-2)
                device_args=(-Dcapabilities.udid=emulator-5556 -Dcapabilities.deviceName=emulator-5556 -Dcapabilities.systemPort=8201 -Dcapabilities.mjpegServerPort=7811)
                ;;
            *)
                printf 'Choose android-1 or android-2 for Android.\n' >&2
                exit 2
                ;;
        esac
        ;;
    ios)
        suite="ios_$coverage"
        device="${device:-ios-1}"
        case "$device" in
            ios-1)
                device_args=(-Dcapabilities.udid=B7C13093-3F0C-4711-8801-0178C4EFECF5 '-Dcapabilities.deviceName=iPhone 17' -Dcapabilities.platformVersion=26.5 -Dcapabilities.useNewWDA=true -Dcapabilities.wdaLocalPort=8100 -Dcapabilities.derivedDataPath=/tmp/qamobile-wda-ios-1)
                ;;
            ios-2)
                device_args=(-Dcapabilities.udid=FFC09482-BDDE-4692-8962-508A2CF062BA '-Dcapabilities.deviceName=iPhone 17 Pro' -Dcapabilities.platformVersion=26.5 -Dcapabilities.useNewWDA=true -Dcapabilities.wdaLocalPort=8101 -Dcapabilities.derivedDataPath=/tmp/qamobile-wda-ios-2)
                ;;
            *)
                printf 'Choose ios-1 or ios-2 for iOS.\n' >&2
                exit 2
                ;;
        esac
        ;;
    *) printf 'Usage: bash run-tests.sh android|ios [smoke|regression|grid|filter|cart|account|login] [local|zebrunner] [--device android-1|android-2|ios-1|ios-2]\n' >&2; exit 2 ;;
esac
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${QAMOBILE_PROJECT:-$project_root}"
case "$reporting" in
    local)
        export REPORTING_ENABLED=false
        ;;
    zebrunner)
        if [ -z "${REPORTING_SERVER_ACCESS_TOKEN:-}" ]; then
            printf 'Set REPORTING_SERVER_ACCESS_TOKEN in your terminal before enabling Zebrunner reporting.\n' >&2
            exit 2
        fi
        export REPORTING_ENABLED=true
        export REPORTING_SERVER_HOSTNAME=https://solvdinternal.zebrunner.com
        export REPORTING_PROJECT_KEY=SAUCEM
        export REPORTING_RUN_DISPLAY_NAME="[$platform][$coverage] Swag Labs - $device - Local MCloud"
        ;;
    *) printf 'Choose local or zebrunner as the third argument.\n' >&2; exit 2 ;;
esac
lock_dir="${TMPDIR:-/tmp}/qamobile-swaglabs-${device}.lock"
if ! mkdir "$lock_dir" 2>/dev/null; then
    printf '%s is already running a test. Choose another device or wait for it to finish.\n' "$device" >&2
    exit 75
fi
cleanup() {
    rmdir "$lock_dir" 2>/dev/null || true
}
trap cleanup EXIT
mvn -B -ntp test "-Dsuite=$suite" "-Dreporting.enabled=$REPORTING_ENABLED" -Dprovider=mcloud \
    -Dselenium_url=http://localhost:4446/wd/hub "${device_args[@]}"
