FROM maven:3.8.1-openjdk-17-slim as build-api
WORKDIR /tmp/api
COPY . .
RUN mvn clean install -DskipTests

# following part 'build-jre' is optional, build a modular JRE limited to what the app is using
# reduce docker image size + improve JRE load time
FROM openjdk:17-alpine as build-jre
WORKDIR /tmp/jre
COPY --from=build-api /tmp/api/target/clin-portal-forms-0.0.1-SNAPSHOT.jar app.jar
RUN unzip app.jar -d unzip
# extract the list of modules from the app.jar
RUN $JAVA_HOME/bin/jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    -q \
    --recursive \
    --multi-release 17 \
    --class-path="./unzip/BOOT-INF/lib/*" \
    --module-path="./unzip/BOOT-INF/lib/*" \
    ./app.jar > modules.info
# build the custom JRE from modules list
RUN apk add --no-cache binutils
RUN $JAVA_HOME/bin/jlink \
    --verbose \
    --add-modules $(cat modules.info) \
    --add-modules jdk.crypto.ec \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output minimal

FROM alpine:latest
WORKDIR /app
ENV JAVA_HOME=/jre
ENV JAVA_OPTS="-XX:+UseZGC -XshowSettings:vm -XX:+PrintCommandLineFlags"
ENV PATH="$PATH:$JAVA_HOME/bin"
RUN apk update && apk add ca-certificates openssl
COPY --from=build-jre /tmp/jre/minimal $JAVA_HOME
COPY --from=build-api /tmp/api/target/clin-portal-forms-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT java $JAVA_OPTS -jar app.jar

# legacy way to run the app without 'build-jre' custom JRE
#FROM openjdk:17-alpine
#WORKDIR /app
#RUN apk update && apk add ca-certificates openssl
#COPY --from=build-api /tmp/api/target/clin-portal-forms-0.0.1-SNAPSHOT.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","app.jar"]
