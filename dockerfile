FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --chown=app:app build/libs/*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]