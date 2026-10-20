FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon :app:installDist

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/app/build/install/app /app

ENV FAMILYPLANNER_PERSISTENCE=memory

USER app
EXPOSE 8080

ENTRYPOINT ["/app/bin/app"]