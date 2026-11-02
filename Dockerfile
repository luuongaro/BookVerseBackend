FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system bookverse && useradd --system --gid bookverse bookverse
COPY --from=build --chown=bookverse:bookverse /build/target/BookVerse-0.0.1-SNAPSHOT.jar /app/bookverse.jar
ENV SPRING_PROFILES_ACTIVE=render \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:+ExitOnOutOfMemoryError"
USER bookverse
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/bookverse.jar"]
