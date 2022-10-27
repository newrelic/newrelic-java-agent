############################################################
# Copyright 2022 New Relic Corporation. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
############################################################

# Install.ps1
#
# Retrieves the latest NuGet package for the Java Agent for
# Azure Web Sites and runs it with default parameters.
# If the agent configuration file is found, only an upgrade
# is performed.

function WriteToInstallLog($output)
{
	$logPath = (Split-Path -Parent $PSCommandPath) + "\install.log"
	Write-Output "[$(Get-Date)] -- $output" | Out-File -FilePath $logPath -Append
}

try {
	WriteToInstallLog "Start executing install.ps1"

	$agentVersion = "current"
	if ($env:NEWRELIC_AGENT_VERSION_OVERRIDE -ne $null)
	{
		$agentVersion = $env:NEWRELIC_AGENT_VERSION_OVERRIDE.ToString()
	}
	Invoke-WebRequest -Uri "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/$agentVersion/newrelic-java.zip" -OutFile newrelic-java.zip
	$newRelicFolder="$HOME\NewRelicJavaAgent"
	Expand-Archive -Path newrelic-java.zip -DestinationPath "$newRelicFolder"
	WriteToInstallLog "End executing install.ps1."
	WriteToInstallLog "-----------------------------"
	exit $LASTEXITCODE
}
catch
{
	$errorMessage = $_.Exception.Message
	WriteToInstallLog $errorMessage
	exit 1
}
