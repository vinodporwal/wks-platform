# Camunda Run 7 with the AOP in-engine task listeners baked in.
#
# The stock camunda/camunda-bpm-platform:run image has none of our classes on its
# classpath. AOP_Approval_v2.bpmn references two listeners via `camunda:class`:
#   - com.wks.bpm.engine.camunda.client.aop.AopRoleCollectionListener
#   - com.wks.bpm.engine.camunda.client.aop.AopGateDecisionListener
# These are instantiated INSIDE the engine when the process starts, so the
# c7-plugins jar must live on the engine's classpath
# (Camunda Run scans configuration/userlib/).
#
# Previously this jar was injected via a docker-compose bind-mount of the local Maven
# `target/` dir. That works for local dev but not on the server, which only pulls
# prebuilt images and never runs `mvn package` -> empty mount -> ENGINE-09017 / CNFE.
# Baking it into the image makes the engine self-contained.
#
# Build context must be the repo root so the COPY below can reach the Maven output
# produced by the pipeline's `mvn -B package --file apps/java/pom.xml` step.
ARG CAMUNDA_DOCKER_IMAGE=camunda/camunda-bpm-platform
ARG CAMUNDA7_VERSION=run-7.20.0
FROM ${CAMUNDA_DOCKER_IMAGE}:${CAMUNDA7_VERSION}

COPY apps/java/libraries/c7-plugins/target/c7-plugins-1.0-SNAPSHOT.jar \
     /camunda/configuration/userlib/c7-plugins-1.0-SNAPSHOT.jar
