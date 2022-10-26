############################################################
# Copyright 2020 New Relic Corporation. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
############################################################

# Install.ps1
#
# Retrieves the latest NuGet package for the .NET Agent for
# Azure Web Sites and runs it with default parameters.
# If the agent configuration file is found, only an upgrade
# is performed.

function WriteToInstallLog($output)
{
	$logPath = (Split-Path -Parent $PSCommandPath) + "\install.log"
	Write-Output "[$(Get-Date)] -- $output" | Out-File -FilePath $logPath -Append
}

function CheckIfAppIs35
{
	
	try
	{
		$xdoc = new-object System.Xml.XmlDocument
		$file = resolve-path($env:APP_POOL_CONFIG)
		$xdoc.load($file)
	
		$appPools = $xdoc.configuration.'system.applicationHost'.applicationPools.add
	
		Foreach($appPool in $appPools)
		{
			if ($appPool.Attributes["managedRuntimeVersion"].Value -eq "v2.0" )
			{
				return $TRUE
			}
		}
	
		return $FALSE
	
	}catch
	{
		WriteToInstallLog "Unable to detect if CLR 2.0 is being used. Failed to install .NET Agent."
		exit 1
	}
}

function CopyDirectory ($fromDirectory, $destinationDirectory)
{
	if (Test-Path $destinationDirectory)
	{
		foreach ($item in Get-ChildItem -Path $fromDirectory) 
		{
			if($item -is [System.IO.DirectoryInfo])
			{
				CopyDirectory "$fromDirectory\$item" "$destinationDirectory\$item"
			}
			else
			{
				Copy-Item "$fromDirectory\$item" -Destination "$destinationDirectory" -Force
			}
		}
	}
	else
	{
		Copy-Item -path $fromDirectory -Destination $destinationDirectory -Force -Recurse -ErrorAction Continue
	}
}

function SaveNewRelicConfigAndCustomInstrumentationFiles($newRelicInstallPath)
{
	if (Test-Path -Path "$newRelicInstallPath")
	{
		if(Test-Path -Path "$newRelicInstallPath\saved_items")
		{
			WriteToInstallLog "Rename existing $newRelicInstallPath\saved_items folder to .$newRelicInstallPath\saved_items-$((Get-Date).ToString('yyyyMMddHHmmss'))"
			Rename-Item "$newRelicInstallPath\saved_items" "$newRelicInstallPath\saved_items-$((Get-Date).ToString('yyyyMMddHHmmss'))" -Force
		}
		WriteToInstallLog "Create new $newRelicInstallPath\saved_items folder."
		New-Item "$newRelicInstallPath\saved_items" -ItemType directory

		if (Test-Path -Path "$newRelicInstallPath\newrelic.config")
		{
			WriteToInstallLog "Save existing newrelic.config to the $newRelicInstallPath\saved_items folder."
			Copy-Item -Path "$newRelicInstallPath\newrelic.config" -Destination "$newRelicInstallPath\saved_items"
		}

		WriteToInstallLog "Save the following existing custom instrumentation files to the $newRelicInstallPath\saved_items folder."
		Get-ChildItem "$newRelicInstallPath\extensions\*.xml" -Exclude "NewRelic.Providers.*" | Out-File -FilePath "$(Split-Path -Parent $PSCommandPath)\install.log" -Append
		Get-ChildItem "$newRelicInstallPath\extensions\*.xml" -Exclude "NewRelic.Providers.*" | Copy-Item -Destination "$newRelicInstallPath\saved_items"
	}
}

function RestoreCustomerRelatedFiles($newRelicInstallPath, $newRelicLegacyInstallPath)
{
	if (Test-Path -Path "$newRelicInstallPath\saved_items")
	{
		WriteToInstallLog "Restore newrelic.config and custom instrumentation files from the saved_items folder."
		Copy-Item -Path "$newRelicInstallPath\saved_items\newrelic.config" -Destination "$newRelicInstallPath\newrelic.config" -Force
		Get-ChildItem "$newRelicInstallPath\saved_items\*.xml" | Copy-Item -Destination "$newRelicInstallPath\extensions"
	}
	# If there aren't any saved_items in the current install path, then this may be an upgrade between versions of the site extension
	# that changed the storage path for where the new relic agent lives. The uninstall of the site extension removes the legacy storage
	# locations, so this migration should only ever happen once.
	elseif (Test-Path -Path $newRelicLegacyInstallPath)
	{
		WriteToInstallLog "Migrating newrelic.config and custom instrumentation files from old site extension storage location."
		if (Test-Path -Path "$newRelicLegacyInstallPath\newrelic.config")
		{
			WriteToInstallLog "Migrating $newRelicLegacyInstallPath\newrelic.config to $newRelicInstallPath\newrelic.config."
			Copy-Item -Path "$newRelicLegacyInstallPath\newrelic.config" -Destination "$newRelicInstallPath\newrelic.config" -Force
		}

		WriteToInstallLog "Migrating the following custom instrumentation files to the $newRelicInstallPath."
		Get-ChildItem "$newRelicLegacyInstallPath\extensions\*.xml" -Exclude "NewRelic.Providers.*" | Out-File -FilePath "$(Split-Path -Parent $PSCommandPath)\install.log" -Append
		Get-ChildItem "$newRelicLegacyInstallPath\extensions\*.xml" -Exclude "NewRelic.Providers.*" | Copy-Item -Destination "$newRelicInstallPath\extensions"
	}
}

function RenameExistingFilesAsSaveExtensionFiles($newRelicInstallPath)
{
	if (Test-Path -Path $newRelicInstallPath)
	{
		WriteToInstallLog "Rename existing Agent files to *.save files except for files in the saved_items folder."
		$toBeRenamedItems = Get-ChildItem -Path "$newRelicInstallPath" -Recurse | Where {$_.FullName -notlike "*\saved_items*"}
		foreach ($item in $toBeRenamedItems)
		{
			if(Test-Path $item.fullname -PathType Leaf)
			{
				Rename-Item $item.fullname "$($item.fullname).save" -Force
			}
		}
	}
}

function RemoveExistingSaveExtensionFiles($newRelicInstallPath)
{
	WriteToInstallLog "Remove existing *.save files from previous upgrade if there are any."
	if(Test-Path $newRelicInstallPath)
	{
		Get-ChildItem $newRelicInstallPath -Include *.save -Recurse | Remove-Item
	}
}

function InstallNewAgent($newRelicNugetContentPath, $newRelicInstallPath, $newRelicLegacyInstallPath)
{
	###Remove existing *.save files from previous upgrade###
	RemoveExistingSaveExtensionFiles $newRelicInstallPath
	
	###Preserve existing newrelic.config and custom instrumemtation xml files###
	SaveNewRelicConfigAndCustomInstrumentationFiles $newRelicInstallPath

	###Rename all existing files as *.save files except for files in the saved_items folder.####
	RenameExistingFilesAsSaveExtensionFiles $newRelicInstallPath

	$xdoc = new-object System.Xml.XmlDocument
	$file = resolve-path(".\" + $newRelicNugetContentPath + "\newrelic.config")
	$xdoc.load($file)

	#Set Agent log location
	$xdoc.configuration.log.SetAttribute("directory", "$env:HOME\LogFiles\NewRelic")
	$xdoc.Save($file)

	WriteToInstallLog "Copy items from $(Resolve-Path $newRelicNugetContentPath) to $newRelicInstallPath"
	CopyDirectory $newRelicNugetContentPath $newRelicInstallPath

	###Restore saved newrelic.config and custom instrumemtation files###
	RestoreCustomerRelatedFiles $newRelicInstallPath $newRelicLegacyInstallPath

	###Remove Linux Grpc library since it won't be used.
	$linuxGrpcLib = "$newRelicInstallPath\libgrpc_csharp_ext.x64.so"
	if(Test-Path $linuxGrpcLib)
	{
		WriteToInstallLog "Remove Linux Grpc library since it won't be used"
		Remove-Item $linuxGrpcLib
	}

	###Remove Linux profiler since it won't be used.
	$linuxProfiler = "$newRelicInstallPath\libNewRelicProfiler.so"
	if(Test-Path $linuxProfiler)
	{
		WriteToInstallLog "Remove Linux profiler since it won't be used"
		Remove-Item $linuxProfiler
	}
}

function RemoveXmlElements($file, $xPaths)
{
	$xdoc = new-object System.Xml.XmlDocument
	$xdoc.load($file)
	foreach ($xPath in $xPaths)
	{
		$elementToBeRemoved = $xdoc.SelectSingleNode($xPath)
		if($elementToBeRemoved -ne $null)
		{
			$elementToBeRemoved.ParentNode.RemoveChild($elementToBeRemoved)
		}
	}
	$xdoc.Save($file)
}

function CopyAgentInfo($agentInfoDestination)
{
	try
	{
		$agentInfoDestinationFilePath = $agentInfoDestination + "\agentinfo.json"

		if(-Not(Test-Path $agentInfoDestinationFilePath -PathType Leaf))
		{
			return
		}

		$agentInfoJson = Get-Content "$agentInfoDestinationFilePath" -Raw | ConvertFrom-Json
		$agentInfoJson | Add-Member -NotePropertyName "azure_site_extension" -NotePropertyValue $true -Force
		$agentInfoJson | ConvertTo-Json -depth 32| set-content "$agentInfoDestinationFilePath"
	}
	catch
	{
		WriteToInstallLog "Failed to configure $agentInfoFilePath  to $agentInfoDestination"
	}
}

function RemoveNewRelicInstallArtifacts($fromDirectory)
{
	WriteToInstallLog "Removing New Relic install artifacts"

	try
	{
		if (Test-Path -Path "$fromDirectory\NewRelicPackage")
		{
			Remove-Item -Recurse NewRelicPackage -ErrorAction Stop
		}
	}
	catch
	{
		WriteToInstallLog "Unable to remove 'NewRelicPackage' directory"
		exit 1
	}

	try
	{
		if (Test-Path -Path "$fromDirectory\NewRelicCorePackage")
		{
			Remove-Item -Recurse NewRelicCorePackage -ErrorAction Stop
		}
	}
	catch
    {
		WriteToInstallLog "Unable to remove 'NewRelicCorePackage' directory"
		exit 1
	}
}

function RemoveDeprecatedInstrumentationFiles($newRelicInstallPath)
{
	# Logging files
	Remove-Item "$newRelicInstallPath\extensions\NewRelic.Providers.Wrapper.Logging.Instrumentation.xml" -ErrorAction Ignore
	Remove-Item "$newRelicInstallPath\extensions\NewRelic.Providers.Wrapper.Logging.dll" -ErrorAction Ignore
}

try
{
	WriteToInstallLog "Start executing install.ps1"

	#Using Tls12
	[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bOR [Net.SecurityProtocolType]::Tls12

	#Loading helper assemblies.
	[Reflection.Assembly]::LoadFile((Get-ChildItem NuGet.Core.dll).FullName)
	[Reflection.Assembly]::LoadFile((Get-ChildItem NewRelic.NuGetHelper.dll).FullName)
	[Reflection.Assembly]::LoadFile((Get-ChildItem Microsoft.Web.XmlTransform.dll).FullName)

	$nugetSource = "https://www.nuget.org/api/v2/"

	$nugetPackageForFrameworkApp = "NewRelic.Azure.WebSites"
	$nugetPackageForCoreApp = "NewRelic.Agent"

	if ($env:PROCESSOR_ARCHITECTURE -ne "x86")
	{
		$nugetPackageForFrameworkApp = "NewRelic.Azure.WebSites.x64"
	}

	$is35App = CheckIfAppIs35
	$agentVersion = ""

	if ($env:NEWRELIC_AGENT_VERSION_OVERRIDE -ne $null)
	{
		try
		{
			$version = [System.Version]$env:NEWRELIC_AGENT_VERSION_OVERRIDE.ToString()
			$agentVersion = $version.ToString()
		}
		catch
		{
			WriteToInstallLog "NEWRELIC_AGENT_VERSION_OVERRIDE environment variable has an incorrect Agent version number. Failed to install."
			exit 1
		}
	}
	elseif ($is35App -eq $TRUE)
	{
		$MAX_6X_AGENT_VERSION = "6.999.999"
		$latest6XPackage = [NewRelic.NuGetHelper.Utils]::FindPackage($nugetPackageForFrameworkApp, $MAX_6X_AGENT_VERSION, $nugetSource)
		$agentVersion = $latest6XPackage.Version
	}
	else
	{
		$latestPackage = [NewRelic.NuGetHelper.Utils]::FindPackage($nugetPackageForFrameworkApp,[NullString]::Value, $nugetSource)
		$agentVersion = $latestPackage.Version
	}


	if ($env:NEWRELIC_LICENSEKEY -eq $null -and $env:NEW_RELIC_LICENSE_KEY -eq $null)
	{
		WriteToInstallLog "The environment variable NEWRELIC_LICENSEKEY or NEW_RELIC_LICENSE_KEY must be set. Please make sure to add one."
	}

	RemoveNewRelicInstallArtifacts "."

	if ([System.Version]$agentVersion -ge [System.Version]"8.17.438")
	{
		$nugetPackageForFrameworkApp = $nugetPackageForCoreApp
	}
	else
	{
		$xPaths = @("/configuration/system.webServer/runtime/environmentVariables/add[@name='COR_PROFILER_PATH_32']",
					"/configuration/system.webServer/runtime/environmentVariables/add[@name='COR_PROFILER_PATH_64']",
					"/configuration/system.webServer/runtime/environmentVariables/add[@name='CORECLR_ENABLE_PROFILING']",
					"/configuration/system.webServer/runtime/environmentVariables/add[@name='CORECLR_PROFILER']",
					"/configuration/system.webServer/runtime/environmentVariables/add[@name='CORECLR_PROFILER_PATH_32']",
					"/configuration/system.webServer/runtime/environmentVariables/add[@name='CORECLR_PROFILER_PATH_64']",
					"/configuration/system.webServer/runtime/environmentVariables/add[@name='CORECLR_NEWRELIC_HOME']")
		$file = resolve-path(".\applicationHost.xdt")
		RemoveXmlElements $file $xPaths
	}

	$packageNames = @($nugetPackageForFrameworkApp, $nugetPackageForCoreApp)
	$stagingFolders = @("NewRelicPackage", "NewRelicCorePackage")
	$newRelicInstallPaths = @("$env:HOME\NewRelicAgent\Framework", "$env:HOME\NewRelicAgent\Core")
	$newRelicLegacyInstallPaths = @("$env:WEBROOT_PATH\newrelic", "$env:WEBROOT_PATH\newrelic_core")
	$newRelicNugetContentPaths = $(".\content\newrelic", ".\contentFiles\any\netstandard2.0\newrelic")

	#Check to see if the old Agent is currently being used

	For ($i=0; $i -lt $packageNames.Count; $i++)
	{
		$packageName = $packageNames[$i]
		$stagingFolder = $stagingFolders[$i]
		$newRelicInstallPath= $newRelicInstallPaths[$i]
		$newRelicLegacyInstallPath = $newRelicLegacyInstallPaths[$i]
		$newRelicNugetContentPath = $newRelicNugetContentPaths[$i]

		if($packageName -eq "NewRelic.Agent" -and [System.Version]$agentVersion -lt [System.Version]"8.17.438")
		{
			WriteToInstallLog "New Relic Site Extension does not install .NET Core Agent version less than 8.17.438"
			Break
		}

		New-Item $stagingFolder -ItemType directory
		cd $stagingFolder
		WriteToInstallLog "Excecute Command: nuget install $packageName -Version $agentVersion -Source $nugetSource"

		# Using Start-Process with the -NoNewWindow switch because running executables within Kudu can often fail with "Window title cannot be longer than 1023 characters". See: https://github.com/projectkudu/kudu/issues/2635.
		Start-Process nuget -ArgumentList "install $packageName -Version $agentVersion -Source $nugetSource" -NoNewWindow -PassThru -Wait

		if (-Not (Test-Path -Path "$packageName.$agentVersion"))
		{
			WriteToInstallLog "$packageName.$agentVersion folder does not exists."
			exit 1
		}

		cd "$packageName.$agentVersion"

		InstallNewAgent $newRelicNugetContentPath $newRelicInstallPath $newRelicLegacyInstallPath

		CopyAgentInfo $newRelicInstallPath

		RemoveDeprecatedInstrumentationFiles $newRelicInstallPath

		cd ..\..
	}

	RemoveNewRelicInstallArtifacts "."

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
