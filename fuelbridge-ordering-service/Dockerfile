FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY fuelbridge-ordering-service/.mvn/ fuelbridge-ordering-service/.mvn/
COPY fuelbridge-ordering-service/mvnw fuelbridge-ordering-service/mvnw
RUN sed -i 's/\r$//' fuelbridge-ordering-service/mvnw && chmod +x fuelbridge-ordering-service/mvnw

# shared-kernel no esta publicado en un repo remoto, hay que instalarlo local antes de compilar el servicio
COPY fuelbridge-shared-kernel/pom.xml fuelbridge-shared-kernel/pom.xml
COPY fuelbridge-shared-kernel/src fuelbridge-shared-kernel/src
RUN ./fuelbridge-ordering-service/mvnw -B -f fuelbridge-shared-kernel/pom.xml clean install -DskipTests

COPY fuelbridge-ordering-service/pom.xml fuelbridge-ordering-service/pom.xml
COPY fuelbridge-ordering-service/src fuelbridge-ordering-service/src
RUN ./fuelbridge-ordering-service/mvnw -B -f fuelbridge-ordering-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/fuelbridge-ordering-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
