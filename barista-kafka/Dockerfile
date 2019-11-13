## Maven package
FROM quay.io/quarkus/centos-quarkus-maven:19.2.1 AS build

COPY barista-kafka/src /usr/src/app/barista-kafka/src
COPY barista-kafka/pom.xml /usr/src/app/barista-kafka

COPY pom.xml /usr/src/app

USER root
RUN chown -R quarkus /usr/src/app
USER quarkus
RUN mvn -f /usr/src/app/barista-kafka/pom.xml -Pnative clean package

## Create Image
FROM registry.access.redhat.com/ubi8/ubi-minimal
WORKDIR /work/
COPY --from=build /usr/src/app/barista-kafka/target/*-runner /work/application
RUN chmod 775 /work
EXPOSE 8090
CMD ["./application", "-Dkafka.bootstrap.servers=kafka:9093"]
