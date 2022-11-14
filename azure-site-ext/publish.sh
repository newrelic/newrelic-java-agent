# This is just a simple shell script that will later be
# used as a template for a GHA job for
# azure site extension uploads.

# Dependencies: .Net 5 and up or .Net Core, Mono, and the Nuget CLI.
NUGET_API_KEY=$1
NUGET_SOURCE=$2
VERSION=1.0.0
NUSPEC_GENERATED="NewRelic.Java.Azure.WebSites.Extension.${VERSION}.nuspec"
sed "s/{VERSION}/${VERSION}/g" NewRelic.Java.Azure.WebSites.Extension.nuspec > "${NUSPEC_GENERATED}"
nuget pack "${NUSPEC_GENERATED}"
dotnet nuget push "NewRelic.Java.Azure.WebSites.Extension.${VERSION}.nupkg" --api-key ${NUGET_API_KEY} --source ${NUGET_SOURCE}
