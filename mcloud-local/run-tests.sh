#!/usr/bin/env bash
set -euo pipefail
platform="${1:-}"
coverage="${2:-smoke}"
reporting="${3:-zebrunner}"
case "$coverage" in
    smoke|regression|grid|filter|cart|account|login) ;;
    *) printf 'Choose smoke, regression, grid, filter, cart, account, or login.\n' >&2; exit 2 ;;
esac
case "$platform" in
    android)
        suite="$coverage"
        device_args=(-Dcapabilities.udid=emulator-5554 -Dcapabilities.deviceName=emulator-5554)
        ;;
    ios)
        suite="ios_$coverage"
        device_args=(-Dcapabilities.udid=B7C13093-3F0C-4711-8801-0178C4EFECF5 '-Dcapabilities.deviceName=iPhone 17' -Dcapabilities.platformVersion=26.5 -Dcapabilities.useNewWDA=true)
        ;;
    *) printf 'Usage: bash run-tests.sh android|ios [smoke|regression|grid|filter|cart|account|login] [local|zebrunner]\n' >&2; exit 2 ;;
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
        export REPORTING_RUN_DISPLAY_NAME="Swag Labs $platform $coverage - Local MCloud"
        ;;
    *) printf 'Choose local or zebrunner as the third argument.\n' >&2; exit 2 ;;
esac
exec mvn -B -ntp test "-Dsuite=$suite" "-Dreporting.enabled=$REPORTING_ENABLED" -Dprovider=mcloud \
    -Dselenium_url=http://localhost:4446/wd/hub "${device_args[@]}"
