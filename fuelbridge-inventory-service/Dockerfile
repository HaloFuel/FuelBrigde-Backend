FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY fuelbridge-inventory-service/.mvn/ fuelbridge-inventory-service/.mvn/
COPY fuelbridge-inventory-service/mvnw fuelbridge-inventory-service/mvnw
RUN chmod +x fuelbridge-inventory-service/mvnw

# shared-kernel no esta publicado en un repo remoto, hay que instalarlo local antes de compilar el servicio
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src
RUN ./fuelbridge-inventory-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

COPY fuelbridge-inventory-service/pom.xml fuelbridge-inventory-service/pom.xml
COPY fuelbridge-inventory-service/src fuelbridge-inventory-service/src
RUN ./fuelbridge-inventory-service/mvnw -B -f fuelbridge-inventory-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/fuelbridge-inventory-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
