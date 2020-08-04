# Contributing

Contributions are always welcome. Before contributing please read the
[code of conduct](./CODE_OF_CONDUCT.md) and [search the issue tracker](issues); your issue may have already been discussed or fixed in `main`. To contribute,
[fork](https://help.github.com/articles/fork-a-repo/) this repository, commit your changes, and [send a Pull Request](https://help.github.com/articles/using-pull-requests/).

Note that our [code of conduct](./CODE_OF_CONDUCT.md) applies to all platforms and venues related to this project; please follow it in all your interactions with the project and its participants.

## Feature Requests

Feature requests should be submitted in the [Issue tracker](../../issues), with a description of the expected behavior & use case, where they’ll remain closed until sufficient interest, [e.g. :+1: reactions](https://help.github.com/articles/about-discussions-in-issues-and-pull-requests/), has been [shown by the community](../../issues?q=label%3A%22votes+needed%22+sort%3Areactions-%2B1-desc).
Before submitting an Issue, please search for similar ones in the
[closed issues](../../issues?q=is%3Aissue+is%3Aclosed+label%3Aenhancement).

## Pull Requests

### Version Support

When contributing, keep in mind that New Relic customers (that's you!) are running many different versions of Java, some of them pretty old. Changes that require the newest version of Java will probably be rejected, especially if they replace something backwards compatible.

Be aware that the instrumentation needs to work with a wide range of versions of the instrumented modules, and that code that looks nonsensical or overcomplicated may be that way for compatibility-related reasons. Read all the comments and check the related tests before deciding whether existing code is incorrect.

If you’re planning on contributing a new feature or an otherwise complex contribution, we kindly ask you to start a conversation with the maintainer team by opening up a Github issue first. 

### Dependencies

As new dependencies have to be shaded and licensed for distribution, any additional dependency would have to add significant value.

### Coding Style Guidelines/Conventions

- Use the [style provided](https://github.com/newrelic/newrelic-java-agent/blob/main/dev-tools/code-style/java-agent-code-style.xml) in the project.
- We encourage you to reduce tech debt you might find in the area. Leave the code better than you found it.

### Testing Guidelines

The tests can be run with `./gradlew test`. Note that this runs unit tests, functional tests, and individual instrumentation module tests, so this is extremely time-consuming.

For more information, refer to the [README](https://github.com/newrelic/newrelic-java-agent#testing).

We expect that all new code has robust unit test coverage, and any functional changes should also be covered by tests. Because the Java agent must support Java 7, we are using JUnit 4. We expect reasonable test coverage for any changed paths, but there’s no specific metric (such as through jacoco) at this time.

## Contributor License Agreement

Keep in mind that when you submit your Pull Request, you'll need to sign the CLA via the click-through using CLA-Assistant. If you'd like to execute our corporate CLA, or if you have any questions, please drop us an email at opensource@newrelic.com.

For more information about CLAs, please check out Alex Russell’s excellent post,
[“Why Do I Need to Sign This?”](https://infrequently.org/2008/06/why-do-i-need-to-sign-this/).

## Slack

We host a public Slack with a dedicated channel for contributors and maintainers of open source projects hosted by New Relic.  If you are contributing to this project, you're welcome to request access to the #oss-contributors channel in the newrelicusers.slack.com workspace.  To request access, see https://newrelicusers-signup.herokuapp.com/.
