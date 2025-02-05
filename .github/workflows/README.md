# Workflows

## Pinning Actions to Commit SHAs Instead of Tags
For security [reasons](https://blog.rafaelgss.dev/why-you-should-pin-actions-by-commit-hash), actions in github workflows should be pinned by a commit
SHA rather than a tag. A node based tool exists to automate this: [pin-github-action](https://github.com/mheap/pin-github-action).

This tool can be run locally whenever a `uses` action is added or change in a workflow file. Instructions exist in the tool's README that explain
installation and execution.

## Test workflows

### Java Functional Tests
[GHA-Functional-Tests.yaml](GHA-Functional-Tests.yaml)

Runs the functional tests.

#### Triggers

- Test Suite workflow call;
- manual.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.

---

### Scala Functional Tests
[GHA-Scala-Functional-Tests.yaml](GHA-Scala-Functional-Tests.yaml)

Run the Scala functional tests.

#### Triggers

- Test Suite workflow call;
- manual.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.

---

### Scala Instrumentation Tests
[GHA-Scala-Instrumentation-Tests.yaml](GHA-Scala-Instrumentation-Tests.yaml)

Runs the Scala instrumentation tests.

#### Triggers

- Test Suite workflow call;
- manual.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.

---

### Unit Tests
[GHA-Unit-Tests.yaml](GHA-Unit-Tests.yaml)

Description

#### Triggers

- Test Suite workflow call;
- manual.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.

---

### Java Instrumentation Tests
[Java-Instrumentation-Tests.yml](Java-Instrumentation-Tests.yml)

Runs the instrumentation tests.

#### Triggers

- Test Suite workflow call;
- manual.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.

---

### Test - AITs
[Test-AITs.yml](Test-AITs.yml)

Runs the AITs.

#### Triggers

- Test Suite workflow call;
- manual.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.
- **ait-ref** - the branch/tag/sha to use for running the AITs.
- **cache-ref** - the branch/tag/sha to use on the cache repos.

---

### Verify Instrumentation
[VerifyInstrumentation.yml](VerifyInstrumentation.yml)

Runs the verify instrumentation.

#### Triggers

- Test Suite workflow call;
- manual.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.

---

### Verify Instrumentation (single)
[VerifyInstrumentation-Single.yml](VerifyInstrumentation-Single.yml)

Runs Verify Instrumentation on a single module.

#### Triggers

- Manual.

#### Parameters

- **module** - the instrumentation module to be verified.
- **ref** - the branch/tag/sha to use for compiling the agent.

---

## Test suites

### Test Suite - PR
[TestSuite-PR.yml](TestSuite-PR.yml)

This workflow runs all the test workflows except for Verify Instrumentation.

#### Triggers

- PRs opened to main;
- push to main;
- manually.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.
- **ait-ref** - the branch/tag/sha to use for running the AITs.
- **cache-ref** - the branch/tag/sha to use on the cache repos.

---

### Test Suite - Release
[TestSuite-Release.yml](TestSuite-Release.yml)

This workflow runs all the test workflows.

#### Triggers

- release creation;
- manually.

#### Parameters

- **agent-ref** - the branch/tag/sha to use for compiling the agent.
- **ait-ref** - the branch/tag/sha to use for running the AITs.
- **cache-ref** - the branch/tag/sha to use on the cache repos.

---

## Miscellaneous workflows

### Create custom jar
[Create-Custom-Jar.yml](Create-Custom-Jar.yml)

This command creates custom jars that we can share with customers.

#### Parameters

- **ref** - the branch/tag/hash that should be checked out to generate the jar;
- **description** - a description that will show on the summary, so we know the contents of that custom jar.

---

### Repolinter Action
[repolinter.yml](repolinter.yml)

This workflow checks that the repository conforms to New Relic standards. It will create issues for non conformities.

---

## Release workflows

### Release - Copy to S3 (Staging)
[Release-CopyToS3-staging.yml](Release-CopyToS3-staging.yml)

Retrieves the release artifacts from the Sonatype staging repo and sends to the Java agent team's S3 bucket for inspection.

#### Parameters

- **repo_name** - the sonatype repo to get the artifact from, eg. comnewrelic-1234
- **version_number** - The version being released, eg. 7.11.0


### Release - Copy to S3 (Prod)
[Release-CopyToS3-prod.yml](Release-CopyToS3-prod.yml)

Retrieves the release artifacts from Maven central and uploads to the downloads website.

#### Parameters

- **version_number** - The version being released, eg. 7.11.0

---

### Publish snapshot on main merge
[publish_main_snapshot.yml](publish_main_snapshot.yml)

This workflow sends a snapshot to Maven Central every time a commit is pushed to main.

#### Triggers

- Push to main.

---

### Publish release version explicitly
[publish_release.yml](publish_release.yml)

This workflow sends release artifacts to Maven Central's staging repo. With this artifact the release process starts.

#### Triggers

- Version created in Github. 

---

## Reusable workflows

### Build agent
[X-Reusable-BuildAgent.yml](X-Reusable-BuildAgent.yml)

This workflow will build the agent and add it to the cache (using the run id as the key). Other jobs in the same workflow can then retrieve the agent from the cache, using the same key.

#### Parameters

- **ref** - the branch/tag/sha to use for compiling the agent.

#### How to use

```yaml
jobs:
  build-agent:
    uses: ./.github/workflows/X-Reusable-BuildAgent.yml
    with:
      ref: ${{ inputs.agent-ref || github.sha }}
    secrets: inherit

  another-job:
    runs-on: ubuntu-24.04
    needs: build-agent
    steps:
      - name: Retrieve agent from cache
        id: retrieve-agent
        uses: actions/cache@v3
        with:
          path: /home/runner/work/newrelic-java-agent/newrelic-java-agent/newrelic-agent/build/newrelicJar/newrelic.jar
          key: ${{ github.run_id }}
      - name: Do something with the agent
#       ...
```

---

### Reusable Verify Instrumentation
[X-Reusable-VerifyInstrumentation.yml](X-Reusable-VerifyInstrumentation.yml)

This workflow is part of [VerifyInstrumentation.yml](VerifyInstrumentation.yml) and not meant to be used elsewhere. It was created because the matrix strategy can run at most 255 items. Since we have around 300 instrumentation modules, they had to be processed in batches. This workflow runs one page (batch), so it is called once for each page in the Verify Instrumentation workflow.

#### Parameters

- **page** - the page of modules to be processed. Eg. '1/2', '2/3'
- **ref** - the branch/tag/sha to use for compiling the agent.
