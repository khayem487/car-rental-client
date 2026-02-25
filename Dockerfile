FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY car-rental-client/pom.xml car-rental-client/pom.xml
COPY car-rental-client/src car-rental-client/src

RUN mvn -f car-rental-client/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/car-rental-client/target/client-0.0.1-SNAPSHOT.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=demo
EXPOSE 10000

CMD ["sh", "-c", "java -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
