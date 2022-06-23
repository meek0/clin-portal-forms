FROM maven:3.6.3-jdk-11-slim as build-hapi

WORKDIR /tmp/clin-prescription-renderer
COPY . .

RUN mvn clean install -DskipTests

FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk update && apk add ca-certificates openssl
COPY --from=build-hapi /tmp/clin-prescription-renderer/target/clin-portal-forms-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]