FROM maven:3.8-openjdk-11 AS builder
RUN mkdir -p /src
ADD ./*.xml /src/
RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "--f", "/src/pom.xml", "dependency:go-offline", "--fail-never", "--s", "/src/user-settings.xml", "-Dmaven.artifact.threads=16", "package", "clean"]
COPY ./ /src/
RUN ["mvn", "package", "--f", "/src/pom.xml", "--s", "/src/user-settings.xml", "-o", "-DskipTests"]

FROM openjdk:11-jre
WORKDIR /app
COPY --from=builder ./src/target/*.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","./app.jar"]
