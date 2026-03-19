FROM eclipse-temurin:21-jdk-alpine

# Membuat group dan user baru agar tidak menggunakan root
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Mengganti pemilik file .jar menjadi user 'spring'
COPY --chown=spring:spring build/libs/*.jar app.jar

# Berpindah dari user root ke user 'spring'
USER spring

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]