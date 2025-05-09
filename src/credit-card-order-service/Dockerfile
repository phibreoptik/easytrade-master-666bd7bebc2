FROM openjdk:17-jdk-slim as build

WORKDIR /src
# make gradlew download gradle files before copying source files to use caching
COPY build.gradle gradlew settings.gradle ./
COPY gradle ./gradle

COPY src /src/src

RUN ./gradlew test
RUN ./gradlew bootJar

FROM openjdk:17-jdk-slim

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY ./rook.jar /rook.jar
COPY --from=build /src/build/libs/*.jar app.jar
ENV JAVA_TOOL_OPTIONS=-javaagent:/rook.jar

ENTRYPOINT ["java","-jar","/app.jar"]