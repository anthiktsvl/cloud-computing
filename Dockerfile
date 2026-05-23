# Build stage
FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY WebContent ./WebContent
RUN mvn clean package -DskipTests

# Runtime stage
FROM tomcat:10.1-jdk17-temurin
ENV INSTANCE_ID=container-instance
COPY --from=build /app/target/MyWebsite.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
