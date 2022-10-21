# Workflows

## Manually runnable workflows

### Create custom jar

This command creates custom jars that we can share with customers.

#### Parameters

- **ref** - the branch/tag/hash that should be checked out to generate the jar;
- **description** - a description that will show on the summary, so we know the contents of that custom jar.


### Z - Sandbox job

This is a hello world workflow to allow development of new workflows without commiting a new blank workflow to `main`. 

To use it, just develop it in your branch and when running the workflow, select to run your branch's version.

This workflow has 3 params. While developing, your new workflow gotta keep those 3 params. You can change it before saving your real workflow.

Lastly, please do not commit changes to this workflow, so the next person can use it without having to clean it up.

## Release workflows

### Release - Copy to S3 (Staging)

#### Parameters

- **repo_name** - the sonatype repo to get the artifact from, eg. comnewrelic-1234
- **version_number** - The version being released, eg. 7.11.0


### Release - Copy to S3 (Prod)

#### Parameters
- 
- **version_number** - The version being released, eg. 7.11.0
