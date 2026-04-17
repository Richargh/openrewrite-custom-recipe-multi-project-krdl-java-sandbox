package de.richargh.sandbox.openrewrite.gitlabci;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class GitLabCiPropertyOrderRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new GitLabCiPropertyOrderRecipe(null, null));
    }

    @Test
    void reordersGlobalKeysToDefaultOrder() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        stages:
                          - build
                          - test
                        image: alpine:latest
                        include:
                          - project: 'my-group/shared-ci'
                            file: '/templates/security.yml'
                        variables:
                          DOCKER_DRIVER: overlay2
                        before_script:
                          - echo "global setup"
                        """,
                        // language=yaml
                        """
                        include:
                          - project: 'my-group/shared-ci'
                            file: '/templates/security.yml'
                        image: alpine:latest
                        stages:
                          - build
                          - test
                        variables:
                          DOCKER_DRIVER: overlay2
                        before_script:
                          - echo "global setup"
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    @Test
    void movesJobDefinitionsAfterGlobalKeys() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        my-job:
                          script: echo hello
                        image: alpine:3.18
                        stages:
                          - build
                        """,
                        // language=yaml
                        """
                        image: alpine:3.18
                        stages:
                          - build
                        my-job:
                          script: echo hello
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    @Test
    void preservesRelativeOrderOfMultipleJobs() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        build-job:
                          script: make build
                          stage: build
                        test-job:
                          script: make test
                          stage: test
                        image: alpine:latest
                        stages:
                          - build
                          - test
                        """,
                        // language=yaml
                        """
                        image: alpine:latest
                        stages:
                          - build
                          - test
                        build-job:
                          stage: build
                          script: make build
                        test-job:
                          stage: test
                          script: make test
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    @Test
    void noChangeWhenGlobalKeysAlreadyOrdered() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        image: alpine:latest
                        stages:
                          - build
                        variables:
                          FOO: bar
                        my-job:
                          script: echo done
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    // -------------------------------------------------------------------------
    // Job key ordering
    // -------------------------------------------------------------------------

    @Test
    void reordersJobKeysToDefaultOrder() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        stages:
                          - build

                        build-job:
                          script:
                            - make build
                          tags:
                            - docker
                          stage: build
                          image: node:20-alpine
                          artifacts:
                            paths:
                              - dist/
                          variables:
                            NODE_ENV: production
                        """,
                        // language=yaml
                        """
                        stages:
                          - build

                        build-job:
                          image: node:20-alpine
                          stage: build
                          variables:
                            NODE_ENV: production
                          script:
                            - make build
                          artifacts:
                            paths:
                              - dist/
                          tags:
                            - docker
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    @Test
    void reordersJobWithExtendsFirst() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        stages:
                          - test

                        .base-job:
                          image: alpine:latest

                        test-job:
                          stage: test
                          script: make test
                          extends: .base-job
                        """,
                        // language=yaml
                        """
                        stages:
                          - test

                        .base-job:
                          image: alpine:latest

                        test-job:
                          extends: .base-job
                          stage: test
                          script: make test
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    @Test
    void noChangeWhenJobKeysAlreadyOrdered() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        stages:
                          - build

                        build-job:
                          image: alpine:latest
                          stage: build
                          script: make
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void doesNotApplyToNonGitlabCiFiles() {
        // Files not named .gitlab-ci.yml should not be modified
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        stages:
                          - build
                        image: alpine:latest
                        """,
                        spec -> spec.path("other.yml")
                )
        );
    }

    @Test
    void handlesEmptyFile() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        image: alpine:latest
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    @Test
    void handlesReferenceTag() {
        rewriteRun(
                yaml(
                        // language=yaml
                        """
                        stages:
                          - build

                        .setup:
                          before_script:
                            - echo "setup"

                        build-job:
                          script:
                            - !reference [.setup, before_script]
                            - make build
                          stage: build
                          image: alpine:latest
                        """,
                        // language=yaml
                        """
                        stages:
                          - build

                        .setup:
                          before_script:
                            - echo "setup"

                        build-job:
                          image: alpine:latest
                          stage: build
                          script:
                            - !reference [.setup, before_script]
                            - make build
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }

    @Test
    void customGlobalOrder() {
        rewriteRun(
                spec -> spec.recipe(new GitLabCiPropertyOrderRecipe(
                        java.util.List.of("stages", "image"),
                        null
                )),
                yaml(
                        // language=yaml
                        """
                        image: alpine:latest
                        stages:
                          - build
                        """,
                        // language=yaml
                        """
                        stages:
                          - build
                        image: alpine:latest
                        """,
                        spec -> spec.path(".gitlab-ci.yml")
                )
        );
    }
}
