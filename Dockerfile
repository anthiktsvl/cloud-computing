# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY src /app/src
COPY WebContent /app/WebContent
RUN mvn -q -DskipTests package

# Runtime stage
FROM tomcat:10.1-jdk17-temurin
ENV INSTANCE_ID=container-instance
COPY --from=build /app/target/MyWebsite.war /usr/local/tomcat/webapps/MyWebsite.war
EXPOSE 8080
