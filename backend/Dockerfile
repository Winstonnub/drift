FROM eclipse-temurin:21-jdk-jammy AS build 
# uses Java 21 image with full JDK

WORKDIR /workspace



COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline
# downloads maven dependencies early

COPY src src

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
# starts a second smaller runtime image

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /workspace/target/*.jar app.jar

USER spring:spring
# runs the app as non-root user

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]