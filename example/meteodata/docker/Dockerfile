FROM ubuntu:latest

RUN apt-get update && apt-get install -y curl jq && rm -rf /var/lib/apt/lists/*

COPY ./java/zulu ./java

COPY ./start.sh .
COPY ./open-meteo-zodiac-*-all.jar ./open-meteo-zodiac-all.jar

RUN chmod +x ./start.sh

EXPOSE 8080
ENTRYPOINT ["./start.sh"]