# Azure Java Agent Site Extension

### Installation of the Site Extension

This extension is designed for Java applications running on Azure Windows compute resources.

**Note:** Make sure that the target application is stopped prior to installing the extension.

From the Azure Home page, do the following:
- Click the App Services tile
- Click the name of the target application in the displayed list
- On the options listed on the left, scroll down to "Extensions" located under the `Development Tools` category
- Click on `+ Add` at the top of the page
- From the extension drop down, select `New Relic Java Agent`
- Click on the `Accept Legal Terms` link
- Click `OK` on the bottom left of the page
- Again, click `OK` on the bottom left of the page. This will begin installation of the extension

Once the Java Agent is installed, you'll need to manually enter two configuration items before restarting your application:
- On the options listed on the left, scroll down to "Configuration" located under the `Settings` category
- On the configuration page, add the following two app settings:
    - `NEW_RELIC_LICENSE_KEY` - Your New Relic license key value
	- `NEW_RELIC_APP_NAME` - The name you wish your application to show up as in the New Relic Platform
	
You can also add any additional [app settings](https://docs.newrelic.com/docs/apm/agents/java-agent/configuration/java-agent-configuration-config-file/#Environment_Variables) to configure the agent as needed.

### Extension Details

Once installed, the extension creates the following artifacts:
- Folder: `C:\home\NewRelic\JavaAgent\newrelic` - Contains the Java agent artifacts (jars, sources, etc)
- XDT: `applicationHost.xdt` that will add the necessary `JAVA_TOOL_OPTIONS` environment variable on application startup

If the extension fails to install, a log file is created at `C:\home\SiteExtensions\NewRelic.Azure.WebSites.Extension.JavaAgent\install.log`.

### Creating the NuGet Package on OS X

#### Tooling Install

- Download and install the latest version of [Mono](https://www.mono-project.com/download/stable/)
- Download `nuget.exe`: `sudo curl -o /usr/local/bin/nuget.exe https://dist.nuget.org/win-x86-commandline/latest/nuget.exe`
- Create an alias in your .bashrc or .zshrc for mono: `alias nuget="mono /usr/local/bin/nuget.exe"`
- Download and install [.Net 6](https://dotnet.microsoft.com/en-us/download/dotnet/6.0). Using the installer will create a `dotnet` command that will be available when you restart your shell.
- Restart your shell and execute `nuget` to verify your mono installation and `dotnet` to verify your .Net installation.

References:
https://www.wiliam.com.au/wiliam-blog/creating-a-nuget-package
https://learn.microsoft.com/en-au/nuget/install-nuget-client-tools#nugetexe-cli

#### Creating the Package

##### Manually Creating the Package

- Change into the folder where the `.nuget` file exists
- Execute: `nuget pack NewRelic.Azure.WebSites.Extension.JavaAgent.nuspec`
- This will create a package with the name: `NewRelic.Azure.WebSites.Extension.JavaAgent.VERSION.nupkg`
- Execute: `dotnet nuget push NewRelic.Java.Azure.WebSites.Extension.nupkg --api-key NUGET_API_KEY --source NUGET_SOURCE` where `NUGET_API_KEY` is your NuGet API key and `NUGET_SOURCE` is the URL of the target NuGet site (https://api.nuget.org/v3/index.json is the main, public URL)

##### Create the Package with the Script
- Run `./publish.sh <NUGET_API_KEY> <NUGET_SOURCE>`: this will create the NuGet package and upload to the target repository
- The parameters for `publish.sh` are the following:
	- `NUGET_API_KEY` - API key for uploading artifacts to the target NuGet repository
	- `NUGET_SOURCE` - Target NuGet repository (https://api.nuget.org/v3/index.json is the main, public URL)

#### Testing with Non-public NuGet Repository

The testing Nuget repository is: https://www.myget.org/

Upload the nuget package then set up an app config variable in Azure:
- `SCM_SITEEXTENSIONS_FEED_URL`: The URL to the private Nuget repository created when registering your myget.org account. For example: https://www.myget.org/F/username-nuget-test/api/v3/index.json

In Azure, when you browse to `Development Tools` > `Extensions`, you will see a list of Nuget packages in your private repository.

### Extension Source Files

Below is a description of the files that make up the extension. This can be helpful for future maintenance on the extension or for the creation of another Site Extension.

- `README.md` - This file
- `NewRelic.Azure.WebSites.Extension.JavaAgent.nuspec` - Contains the metadata about the target extension: Name, authors, copyright, etc. [Nuspec Format](https://learn.microsoft.com/en-us/nuget/reference/nuspec)
- `publish.sh` - Simple script to package the script and upload to the Nuget repository
- `Content/applicationHost.xdt` - XDT transformation to add the necessary agent startup environment variable to the app config when the app starts up
- `Content/install.cmd` - Simple batch file that wraps a call to the Powershell `install.ps1` script
- `Content/install.ps1` - Powershell script that downloads the agent bundle and installs it to the proper location on the host
- `Content/uninstall.cmd` - Simple batch file that will remove the Java installtion artifacts when the extension is removed


