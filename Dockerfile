# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew build -x test -x checkstyleMain -x checkstyleTest

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java","-jar","app.jar"]