FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:copy-dependencies

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV PORT=6767

COPY --from=build /app/target/classes ./target/classes
COPY --from=build /app/target/dependency ./target/dependency
COPY database ./database
COPY uploads ./uploads

EXPOSE 6767

CMD ["java", "-cp", "target/classes:target/dependency/*", "back"]
