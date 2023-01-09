FROM maven:3.8.1-openjdk-17-slim as build-hapi

WORKDIR /tmp/clin-prescription-renderer
COPY . .

RUN mvn clean install -DskipTests

FROM openjdk:17-alpine
RUN apk update && apk add ca-certificates openssl
COPY --from=build-hapi /tmp/clin-prescription-renderer/target/clin-portal-forms-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]