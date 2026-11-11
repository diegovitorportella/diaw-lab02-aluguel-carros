# Back-end DriveJá (Spring Boot, Java 25) — imagem para publicação (Render, Railway, Fly.io...)

# --- build ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# normaliza o fim de linha do wrapper (arquivo pode ter sido salvo com CRLF no Windows)
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# --- execução ---
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
# memória enxuta para planos gratuitos (512 MB)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
