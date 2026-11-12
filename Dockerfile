FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY backend/src src
RUN mkdir classes && javac --release 17 -encoding UTF-8 -d classes src/*.java

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 gunny
COPY --from=build /app/classes /app/classes
COPY backend/assets/ /app/assets1/
COPY public/ /app/public/
USER gunny
ENV PORT=8080
ENV TCP_PORT=22122
EXPOSE 8080 22122
CMD ["java","-Xms32m","-Xmx256m","-Djava.awt.headless=true","-cp","classes","GunnyServer"]
