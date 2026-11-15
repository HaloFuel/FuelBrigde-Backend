FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY fuelbridge-payment-service/.mvn/ fuelbridge-payment-service/.mvn/
COPY fuelbridge-payment-service/mvnw fuelbridge-payment-service/mvnw
RUN chmod +x fuelbridge-payment-service/mvnw

# shared-kernel no esta publicado en un repo remoto, hay que instalarlo local antes de compilar el servicio
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src
RUN ./fuelbridge-payment-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

COPY fuelbridge-payment-service/pom.xml fuelbridge-payment-service/pom.xml
COPY fuelbridge-payment-service/src fuelbridge-payment-service/src
RUN ./fuelbridge-payment-service/mvnw -B -f fuelbridge-payment-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/fuelbridge-payment-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
