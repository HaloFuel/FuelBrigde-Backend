# ---- Build stage ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Maven Wrapper de Catalog
COPY fuelbridge-catalog-service/.mvn/ fuelbridge-catalog-service/.mvn/
COPY fuelbridge-catalog-service/mvnw fuelbridge-catalog-service/mvnw
RUN chmod +x fuelbridge-catalog-service/mvnw

# Instalar shared-kernel 1.1.0
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src

RUN ./fuelbridge-catalog-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

# Compilar Catalog Service
COPY fuelbridge-catalog-service/pom.xml fuelbridge-catalog-service/pom.xml
COPY fuelbridge-catalog-service/src fuelbridge-catalog-service/src

RUN ./fuelbridge-catalog-service/mvnw -B -f fuelbridge-catalog-service/pom.xml clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/fuelbridge-catalog-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
