#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v docker >/dev/null 2>&1; then
    export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
fi
curl --fail --silent --show-error --max-time 5 http://localhost:4723/status >/dev/null
docker compose up -d
for attempt in {1..60}; do
    if curl --fail --silent --max-time 3 http://localhost:4446/wd/hub/status >/dev/null; then
        for platform in android ios; do
            curl --fail --silent --show-error --max-time 10 \
                -H 'Content-Type: application/json' \
                --data-binary "@$platform.json" \
                http://localhost:4446/grid/register
            printf '\n'
        done
        printf 'MCloud Grid: http://localhost:4446/grid/console\nTest endpoint: http://localhost:4446/wd/hub\n'
        exit 0
    fi
    sleep 2
done
printf 'MCloud Grid did not become available. Run docker compose logs grid.\n' >&2
exit 1
