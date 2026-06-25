FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV JAVA_OPTS=""
ENV SERVER_PORT=8888

VOLUME ["/app/config"]

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8888

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
