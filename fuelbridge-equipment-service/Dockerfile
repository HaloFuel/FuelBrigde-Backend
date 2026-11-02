# ---- Build stage ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Maven wrapper primero para aprovechar la cache de capas
COPY fuelbridge-equipment-service/.mvn/ fuelbridge-equipment-service/.mvn/
COPY fuelbridge-equipment-service/mvnw fuelbridge-equipment-service/mvnw
RUN chmod +x fuelbridge-equipment-service/mvnw

# fuelbridge-shared-kernel es una libreria local (no publicada en un repositorio Maven remoto),
# asi que se instala primero en el repo .m2 de esta imagen usando el wrapper del servicio.
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src
RUN ./fuelbridge-equipment-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

# Codigo y empaquetado del servicio (sin tests para acelerar el deploy)
COPY fuelbridge-equipment-service/pom.xml fuelbridge-equipment-service/pom.xml
COPY fuelbridge-equipment-service/src fuelbridge-equipment-service/src
RUN ./fuelbridge-equipment-service/mvnw -B -f fuelbridge-equipment-service/pom.xml clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/fuelbridge-equipment-service/target/*.jar app.jar

# La plataforma (Railway/Render/etc.) inyecta el puerto via $PORT; el perfil "mysql" lo lee con server.port=${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
