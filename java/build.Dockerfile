FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y && \
    apt-get install -y \
    bash curl wget gnupg2 \
    openjdk-8-jdk-headless git python3

RUN mkdir -p /work/src
ADD [ ".", "/work/src" ]

WORKDIR /work/src/java
RUN chmod +x ./gradlew && \
    ./gradlew build

