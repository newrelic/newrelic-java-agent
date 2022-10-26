:: Copyright 2022 New Relic Corporation. All rights reserved.
:: SPDX-License-Identifier: Apache-2.0

SET NEW_RELIC_FOLDER="%HOME%\NewRelicAgent"
IF EXIST %NEW_RELIC_FOLDER% (
  rd /S /q %NEW_RELIC_FOLDER%
)

:: Clean up legacy install locations (unfortunately the same location that the nuget agent is deployed)
SET NEW_RELIC_FOLDER="%WEBROOT_PATH%\newrelic"
IF EXIST %NEW_RELIC_FOLDER% (
  rd /S /q %NEW_RELIC_FOLDER%
)

SET NEW_RELIC_FOLDER="%WEBROOT_PATH%\newrelic_core"
IF EXIST %NEW_RELIC_FOLDER% (
  rd /S /q %NEW_RELIC_FOLDER%
)
