FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY src src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=server
ENV JAVA_OPTS="-Xms128m -Xmx512m"

COPY --from=build /workspace/build/libs/church-management-*.jar /app/church-management.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/church-management.jar"]
