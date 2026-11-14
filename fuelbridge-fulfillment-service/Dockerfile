FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY fuelbridge-fulfillment-service/.mvn/ fuelbridge-fulfillment-service/.mvn/
COPY fuelbridge-fulfillment-service/mvnw fuelbridge-fulfillment-service/mvnw
RUN chmod +x fuelbridge-fulfillment-service/mvnw

# shared-kernel no esta publicado en un repo remoto, hay que instalarlo local antes de compilar el servicio
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src
RUN ./fuelbridge-fulfillment-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

COPY fuelbridge-fulfillment-service/pom.xml fuelbridge-fulfillment-service/pom.xml
COPY fuelbridge-fulfillment-service/src fuelbridge-fulfillment-service/src
RUN ./fuelbridge-fulfillment-service/mvnw -B -f fuelbridge-fulfillment-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/fuelbridge-fulfillment-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
