FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY fuelbridge-equipment-service/.mvn/ fuelbridge-equipment-service/.mvn/
COPY fuelbridge-equipment-service/mvnw fuelbridge-equipment-service/mvnw
RUN chmod +x fuelbridge-equipment-service/mvnw

# shared-kernel no esta publicado en un repo remoto, hay que instalarlo local antes de compilar el servicio
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src
RUN ./fuelbridge-equipment-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

COPY fuelbridge-equipment-service/pom.xml fuelbridge-equipment-service/pom.xml
COPY fuelbridge-equipment-service/src fuelbridge-equipment-service/src
RUN ./fuelbridge-equipment-service/mvnw -B -f fuelbridge-equipment-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/fuelbridge-equipment-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
