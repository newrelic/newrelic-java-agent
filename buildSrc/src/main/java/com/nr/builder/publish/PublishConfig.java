package com.nr.builder.publish;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.plugins.signing.SigningExtension;

import java.net.URI;
import java.util.concurrent.Callable;

public class PublishConfig {
    /**
     * Configures the {@link PublishingExtension} and {@link SigningExtension} on the given project.
     * @param project The gradle project to configure for publishing.
     * @param name The name of the artifact to display in the POM.
     * @param description The description of the artifact to display in the POM.
     * @param setArtifacts An {@link Action} called to configure the published artifacts.
     */
    public static void config(Project project, String name, String description, Action<? super MavenPublication> setArtifacts) {
        config("mavenJava", project, name, description, setArtifacts);
    }

    public static void config(String containerName, Project project, String name, String description, Action<? super MavenPublication> setArtifacts) {
        project.getExtensions().configure(PublishingExtension.class, ext -> {

            ext.publications(container ->
                    container.create(containerName, MavenPublication.class, publication -> {
                        setArtifacts.execute(publication);
                        publication.pom(pom -> {
                            pom.getName().set(name);
                            pom.getDescription().set(description);
                            configurePom(pom);
                        });

                        configureSigning(project, publication);
                    })
            );
            ext.repositories(handler ->
                    handler.maven(repo -> {
                        String projectVersion = project.getVersion().toString();
                        configureRepo(repo, projectVersion);
                    })
            );
        });
    }
    @SuppressWarnings("UnstableApiUsage") // useInMemoryPgpKeys
    private static void configureSigning(Project project, MavenPublication publication) {
        project.getExtensions().configure(SigningExtension.class, ext -> {
            String signingKeyId = (String) project.findProperty("signingKeyId");
            String signingKey = (String) project.findProperty("signingKey");
            String signingKeyPassword = (String) project.findProperty("signingPassword");
            ext.useInMemoryPgpKeys(signingKeyId, signingKey, signingKeyPassword);
            ext.setRequired((Callable<Boolean>) () -> project.getGradle().getTaskGraph().hasTask("uploadArchives"));
            ext.sign(publication);
        });
    }

    private static void configureRepo(MavenArtifactRepository repo, String projectVersion) {
        URI releasesRepoUri = URI.create("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/");
        URI snapshotsRepoUrl = URI.create("https://central.sonatype.com/repositories/maven-snapshots/");
        repo.setUrl(
                projectVersion.endsWith("SNAPSHOT")
                        ? snapshotsRepoUrl
                        : releasesRepoUri
        );
        repo.credentials(creds -> {
            creds.setUsername(System.getenv("SONATYPE_USERNAME"));
            creds.setPassword(System.getenv("SONATYPE_PASSWORD"));
        });
    }

    private static void configurePom(MavenPom pom) {
        pom.getUrl().set("https://github.com/newrelic/newrelic-java-agent");
        pom.licenses(spec -> spec.license(license -> {
            license.getName().set("The Apache License, Version 2.0");
            license.getUrl().set("http://www.apache.org/licenses/LICENSE-2.0.txt");
            license.getDistribution().set("repo");
        }));
        pom.developers(spec -> {
            spec.developer(dev -> {
                dev.getId().set("newrelic");
                dev.getName().set("New Relic");
                dev.getEmail().set("opensource@newrelic.com");
            });
        });
        pom.scm(scm -> {
            scm.getUrl().set("git@github.com:newrelic/newrelic-java-agent.git");
            scm.getConnection().set("scm:git@github.com:newrelic/newrelic-java-agent.git");
        });
    }
}
