FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# kopiujemy cały projekt (parent + moduł backendu + frontend itp.)
COPY . .

# budujemy tylko moduł backendu, ale ciągnąc zależności z parenta
RUN mvn -B -pl invoicedoc-backend -am package -DskipTests

# ---------------- RUNTIME ----------------
FROM eclipse-temurin:21-jre

RUN groupadd -r app && useradd -r -g app -s /usr/sbin/nologin app

WORKDIR /app

# kopiujemy wygenerowany jar backendu
COPY --from=build /workspace/invoicedoc-backend/target/*.jar app.jar

RUN chown -R app:app /app
USER app

ENV JAVA_OPTS=""

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
