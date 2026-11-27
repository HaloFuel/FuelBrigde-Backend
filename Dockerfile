# ---- Build stage ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Maven Wrapper del monorepo
COPY .mvn/ .mvn/
COPY mvnw ./mvnw
RUN chmod +x mvnw

# Instalar shared-kernel 1.1.0 antes del monolito
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src

RUN ./mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

# Compilar FuelBridge Platform
COPY pom.xml ./pom.xml
COPY src ./src

RUN ./mvnw -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
