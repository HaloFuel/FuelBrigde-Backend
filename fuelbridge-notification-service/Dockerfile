# ---- Build stage ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Maven Wrapper de Notification
COPY fuelbridge-notification-service/.mvn/ fuelbridge-notification-service/.mvn/
COPY fuelbridge-notification-service/mvnw fuelbridge-notification-service/mvnw
RUN chmod +x fuelbridge-notification-service/mvnw

# Instalar shared-kernel 1.1.0
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src

RUN ./fuelbridge-notification-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

# Compilar Notification Service
COPY fuelbridge-notification-service/pom.xml fuelbridge-notification-service/pom.xml
COPY fuelbridge-notification-service/src fuelbridge-notification-service/src

RUN ./fuelbridge-notification-service/mvnw -B -f fuelbridge-notification-service/pom.xml clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/fuelbridge-notification-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
