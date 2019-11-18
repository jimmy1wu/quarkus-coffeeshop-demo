## Maven package
FROM quay.io/quarkus/centos-quarkus-maven:19.2.1

WORKDIR /project

RUN mkdir coffeeshop-service
COPY coffeeshop-service/src coffeeshop-service/src
COPY coffeeshop-service/pom.xml coffeeshop-service/pom.xml
COPY pom.xml .
RUN mkdir barista-kafka
RUN mkdir barista-http
COPY barista-kafka/pom.xml barista-kafka/pom.xml
COPY barista-http/pom.xml barista-http/pom.xml

WORKDIR /project/coffeeshop-service

EXPOSE 8080

CMD ["mvn", "compile", "quarkus:dev", "-Dkafka.bootstrap.servers=kafka:9093", "-Dme.escoffier.quarkus.coffeeshop.http.BaristaService/mp-rest/url=http://barista-http_1:8082"]
