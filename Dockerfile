FROM openjdk:21-jre-slim

WORKDIR /app

COPY . .

RUN chmod +x gradlew

RUN ./gradlew build --no-daemon

EXPOSE 8080

CMD ["java", "-jar", "build/libs/keyattestation-all.jar"]
