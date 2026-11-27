# ---- Build stage ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Maven Wrapper del API Gateway
COPY fuelbridge-api-gateway/.mvn/ fuelbridge-api-gateway/.mvn/
COPY fuelbridge-api-gateway/mvnw fuelbridge-api-gateway/mvnw
RUN chmod +x fuelbridge-api-gateway/mvnw

# Compilar API Gateway
COPY fuelbridge-api-gateway/pom.xml fuelbridge-api-gateway/pom.xml
COPY fuelbridge-api-gateway/src fuelbridge-api-gateway/src

RUN ./fuelbridge-api-gateway/mvnw -B -f fuelbridge-api-gateway/pom.xml clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/fuelbridge-api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
