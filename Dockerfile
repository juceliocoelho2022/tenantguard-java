FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && groupadd --gid 10001 tenantguard \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin tenantguard \
    && mkdir -p /app/logs \
    && chown -R 10001:10001 /app \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=10001:10001 /app/target/tenantguard-java-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD curl --fail --silent --show-error http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
