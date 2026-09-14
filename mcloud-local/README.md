# Local MCloud Grid for Swag Labs

This setup runs Zebrunner MCloud Grid in Docker and routes Android and iOS sessions to the existing Appium 3 server on the Mac. It is a simulator/emulator execution setup, not the complete STF device-management UI or a deployment of mcloud-agent/mcloud-ios.

The iOS simulator and Android emulator run on macOS. Keep Appium running on port 4723 and both virtual devices booted. Swag Labs must be installed on each device.

## Start

Run `bash start.sh` from this directory. It starts Docker services and registers the two devices. Run it again after restarting Grid, when no tests are active, to restore registrations.

Open http://localhost:4446/grid/console to view nodes. The test endpoint is http://localhost:4446/wd/hub.

The published Grid 2.6 image is AMD64 and runs using Docker Desktop emulation on this Apple Silicon Mac. The image digest is pinned. Nginx translates Grid's /wd/hub paths to the local Appium root path and reuses upstream connections.

## Tests

Run sequentially from this directory:

```bash
bash run-tests.sh android smoke
bash run-tests.sh ios smoke
bash run-tests.sh android regression
bash run-tests.sh ios regression
```

The wrapper targets the project root, one directory above this folder. Override QAMOBILE_PROJECT to use another checkout. It passes provider=mcloud, the Grid URL, an explicit UDID, and sends every supported suite to Zebrunner by default. The iOS command requests a fresh WebDriverAgent launch because the initial pilot encountered a stale WDA connection on port 8100. This can increase startup time. The allowed suite list excludes retry_demo. Feature selections are grid, filter, cart, account, and login.

The two regression suites cover the main test cases; running every feature XML as well repeats coverage. Concurrent execution is not validated by this setup.

Maven/TestNG reports are written to the test project's target/surefire-reports directory, Carina reports to target/reports, and Allure results to target/allure-results. Copy results between platform runs if separate archives are needed. Generate an Allure report with `mvn allure:report` in the test project.

## Zebrunner reporting

Get your personal access token from Zebrunner Account and profile, API Access. In your Mac's zsh terminal, enter it without displaying it or saving its value in shell history:

```zsh
read -rs 'REPORTING_SERVER_ACCESS_TOKEN?Zebrunner access token: '; printf '\n'
export REPORTING_SERVER_ACCESS_TOKEN
bash run-tests.sh android smoke
bash run-tests.sh ios smoke
unset REPORTING_SERVER_ACCESS_TOKEN
```

Wait for Android to finish before starting iOS. Keep Appium, Docker, and both devices running. Every supported suite reports to https://solvdinternal.zebrunner.com in project SAUCEM by default. Launch names identify the platform and suite. A token is required through the environment; do not save it in project files. Pass `local` as the third argument only when you intentionally want to suppress reporting. Execution still uses the local MCloud Grid and local devices. View reported launches at https://solvdinternal.zebrunner.com/projects/SAUCEM.

## Inspect and stop

```bash
docker compose ps
docker compose logs --tail 100 grid appium-bridge
docker compose down
```

Stopping these services leaves the host Appium server and virtual devices running.

## Assignment differences

The current mcloud-agent Mac role discovers physical devices through usbmuxd; the inspected code contains no simulator registration path. Its task file already uses include_tasks. The older mcloud-ios repository includes simulator code but requires Appium 1.22.3 and is decommissioned. Neither installer was run.

A physical-device WDA IPA is not the simulator build. This setup uses Appium/Xcode's simulator WebDriverAgent. The Swag Labs suites test the native app, not Safari; a separate mobile-web suite is still needed for the assignment's Safari requirement.

Sources: https://github.com/zebrunner/mcloud-grid, https://github.com/zebrunner/mcloud-agent, https://github.com/zebrunner/mcloud-ios.
