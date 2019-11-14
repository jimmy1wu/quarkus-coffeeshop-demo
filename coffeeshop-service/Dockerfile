## Maven package
FROM quay.io/quarkus/centos-quarkus-maven:19.2.1 AS build

COPY coffeeshop-service/src /usr/src/app/coffeeshop-service/src
COPY coffeeshop-service/pom.xml /usr/src/app/coffeeshop-service

COPY pom.xml /usr/src/app

USER root
RUN chown -R quarkus /usr/src/app
USER quarkus
RUN mvn -f /usr/src/app/coffeeshop-service/pom.xml -Pnative clean package

## Create Image
FROM registry.access.redhat.com/ubi8/ubi-minimal
WORKDIR /work/
COPY --from=build /usr/src/app/coffeeshop-service/target/*-runner /work/application
RUN chmod 775 /work
EXPOSE 8080
CMD ["./application", "-Dkafka.bootstrap.servers=kafka:9093", "-Dme.escoffier.quarkus.coffeeshop.http.BaristaService/mp-rest/url=http://barista-http_1:8082"]
