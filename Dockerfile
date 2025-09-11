# Stage 1: Build the Spring Boot app
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests


# Stage 2: Create the runtime image
FROM eclipse-temurin:21-alpine-3.21
MAINTAINER rentEstate
WORKDIR /app
RUN apk update && apk add curl
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /build/target/rentEstate-0.0.1-SNAPSHOT.jar ./application.jar
RUN chown appuser /app/application.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]