FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser && mkdir -p /app/data && chown -R 10001:10001 /app
COPY --from=build /workspace/target/sim-activation-0.1.0-SNAPSHOT.jar /app/app.jar
USER 10001
VOLUME ["/app/data"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
