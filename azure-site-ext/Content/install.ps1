############################################################
# Copyright 2022 New Relic Corporation. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
############################################################

# Install.ps1
#
# Retrieves the latest NuGet package for the Java Agent for
# Azure Web Sites and runs it with default parameters.

function WriteToInstallLog($output)
{
	$logPath = (Split-Path -Parent $PSCommandPath) + "\install.log"
	Write-Output "[$(Get-Date)] -- $output" | Out-File -FilePath $logPath -Append
}

try {
	WriteToInstallLog "Start executing install.ps1"

	# Selects the agent version
	$agentVersion = "current"
	if ($env:NEWRELIC_AGENT_VERSION_OVERRIDE -ne $null) {
		$agentVersion = $env:NEWRELIC_AGENT_VERSION_OVERRIDE.ToString()
		WriteToInstallLog "Installing java agent version $agentVersion"
	} else {
		WriteToInstallLog "Installing the latest java agent"
	}

	# Downloads the java agent zip file
	WriteToInstallLog "Begin downloading newrelic-java.zip"

	# Makes sure web requests use TLS 1.2 protocol
	[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
	# Prevent the progress meter from trying to access the console mode
	$ProgressPreference = "SilentlyContinue"

	# Downloading the new relic zip file
	$null | Invoke-WebRequest -Uri "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/$agentVersion/newrelic-java.zip" -OutFile newrelic-java.zip > $null

	WriteToInstallLog "Finish downloading newrelic-java.zip"

	# Expands the java agent zip file and copies it to the java agent folder
	$NewRelicJavaAgentPath = "$env:HOME\NewRelic\JavaAgent"
	WriteToInstallLog "Expanding newrelic-java.zip"
	Expand-Archive -Path newrelic-java.zip -DestinationPath "$NewRelicJavaAgentPath"
	WriteToInstallLog "Wrote the java agent to $NewRelicJavaAgentPath"

	WriteToInstallLog "End executing install.ps1."
	WriteToInstallLog "-----------------------------"
	exit $LASTEXITCODE
}
catch
{
	$errorMessage = $_.Exception.Message
	$errorLine = $_.InvocationInfo.ScriptLineNumber
	WriteToInstallLog "Error at line $errorLine : $errorMessage"
	exit 1
}
