:: Copyright 2022 New Relic Corporation. All rights reserved.
:: SPDX-License-Identifier: Apache-2.0

SET NEW_RELIC_FOLDER="%HOME%\NewRelic\JavaAgent"
IF EXIST %NEW_RELIC_FOLDER% (
  rd /S /q %NEW_RELIC_FOLDER%
)
