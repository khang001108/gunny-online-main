FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY backend/src src
RUN mkdir classes && javac --release 17 -encoding UTF-8 -d classes src/*.java

FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.version="7.9.22"
WORKDIR /app
RUN useradd --system --uid 10001 gunny && mkdir -p /app/data && chown gunny /app/data
VOLUME ["/app/data"]
ENV GUNNY_BALANCE_FILE=/app/data/balance.properties
COPY --from=build /app/classes /app/classes
COPY backend/assets/ /app/assets1/
COPY backend/tribal.properties /app/tribal.properties
COPY public/ /app/public/
# Never publish an old/missing Java ME build under a new backend version.
RUN cd /app/public/downloads \
 && test -s Gunny-E72-2009-Online-7922.jar \
 && test -s Gunny-E6-Online-7922.jar \
 && test -s Gunny-E72-2009-Online-7922.jad \
 && test -s Gunny-E6-Online-7922.jad \
 && test "$(wc -l < Gunny-7922-SHA256.txt)" -eq 4 \
 && grep -q '  Gunny-E72-2009-Online-7922.jar$' Gunny-7922-SHA256.txt \
 && grep -q '  Gunny-E72-2009-Online-7922.jad$' Gunny-7922-SHA256.txt \
 && grep -q '  Gunny-E6-Online-7922.jar$' Gunny-7922-SHA256.txt \
 && grep -q '  Gunny-E6-Online-7922.jad$' Gunny-7922-SHA256.txt \
 && sha256sum -c Gunny-7922-SHA256.txt \
 && grep -q '^MIDlet-Version: 7\.9\.22' Gunny-E72-2009-Online-7922.jad \
 && grep -q '^MIDlet-Version: 7\.9\.22' Gunny-E6-Online-7922.jad
USER gunny
ENV PORT=8080
ENV TCP_PORT=22122
EXPOSE 8080 22122
CMD ["java","-Xms32m","-Xmx256m","-Djava.awt.headless=true","-cp","classes","GunnyServer"]
