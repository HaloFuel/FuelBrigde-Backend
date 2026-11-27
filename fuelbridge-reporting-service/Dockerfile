# ---- Build stage ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Maven Wrapper de Reporting
COPY fuelbridge-reporting-service/.mvn/ fuelbridge-reporting-service/.mvn/
COPY fuelbridge-reporting-service/mvnw fuelbridge-reporting-service/mvnw
RUN chmod +x fuelbridge-reporting-service/mvnw

# Instalar shared-kernel 1.1.0
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src

RUN ./fuelbridge-reporting-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

# Compilar Reporting Service
COPY fuelbridge-reporting-service/pom.xml fuelbridge-reporting-service/pom.xml
COPY fuelbridge-reporting-service/src fuelbridge-reporting-service/src

RUN ./fuelbridge-reporting-service/mvnw -B -f fuelbridge-reporting-service/pom.xml clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/fuelbridge-reporting-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
