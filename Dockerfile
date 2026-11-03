FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY backend/src src
RUN mkdir classes && javac --release 17 -encoding UTF-8 -d classes src/Battle.java src/Json.java src/CloudStore.java src/GunnyServer.java

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 gunny
COPY --from=build /app/classes /app/classes
COPY backend/assets/land0.png backend/assets/land1.png backend/assets/land2.png /app/assets1/
USER gunny
ENV PORT=8080
EXPOSE 8080
CMD ["java","-Xms32m","-Xmx256m","-Djava.awt.headless=true","-cp","classes","GunnyServer"]
