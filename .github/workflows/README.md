# Workflows

## Manually runnable workflows

### Create custom jar

This command creates custom jars that we can share with customers.

#### Parameters

- **ref** - the branch/tag/hash that should be checked out to generate the jar;
- **description** - a description that will show on the summary, so we know the contents of that custom jar.


## Release workflows

### Release - Copy to S3 (Staging)

Retrieves the release artifacts from the Sonatype staging repo and sends to the Java agent team's S3 bucket for inspection.

#### Parameters

- **repo_name** - the sonatype repo to get the artifact from, eg. comnewrelic-1234
- **version_number** - The version being released, eg. 7.11.0


### Release - Copy to S3 (Prod)

Retrieves the release artifacts from Maven central and uploads to the downloads website.

#### Parameters

- **version_number** - The version being released, eg. 7.11.0
