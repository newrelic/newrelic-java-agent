# Azure Java Agent Site Extension

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
- Execute: `nuget pack NewRelic.Java.Azure.WebSites.Extension.nuspec`
- This will create a package with the name: `NewRelic.Java.Azure.WebSites.Extension.VERSION.nupkg`
- Execute: `dotnet nuget push NewRelic.Java.Azure.WebSites.Extension.nupkg --api-key NUGET_API_KEY --source NUGET_SOURCE` where `NUGET_API_KEY` is your NuGet API key and `NUGET_SOURCE` is the URL of the target NuGet site (https://api.nuget.org/v3/index.json is the main, public URL)

##### Create the Package with the Script
- Export the following environment variables in your shell/environment:
    - `VERSION` - Version of extension
	- `NUGET_API_KEY` - API key for uploading artifacts to the target NuGet repository
	- `NUGET_SOURCE` - Target NuGet repository (https://api.nuget.org/v3/index.json is the main, public URL)
- Run `./publish.sh`: this will create the NuGet package and upload to the target repository

#### Testing with Non-public NuGet Repository

The testing Nuget repository is: https://www.myget.org/

Upload the nuget package then set up an app config variable in Azure:
- `SCM_SITEEXTENSIONS_FEED_URL`: The URL to the private Nuget repository created when registering your myget.org account. For example: https://www.myget.org/F/username-nuget-test/api/v3/index.json

In Azure, when you browse to `Development Tools` > `Extensions`, you will see a list of Nuget packages in your private repository.