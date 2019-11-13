## Maven package
FROM quay.io/quarkus/centos-quarkus-maven:19.2.1 AS build

COPY barista-http/src /usr/src/app/barista-http/src
COPY barista-http/pom.xml /usr/src/app/barista-http

COPY pom.xml /usr/src/app

USER root
RUN chown -R quarkus /usr/src/app
USER quarkus
RUN mvn -f /usr/src/app/barista-http/pom.xml -Pnative clean package

## Create Image
FROM registry.access.redhat.com/ubi8/ubi-minimal
WORKDIR /work/
COPY --from=build /usr/src/app/barista-http/target/*-runner /work/application
RUN chmod 775 /work
EXPOSE 8082
CMD ["./application"]
