FROM adoptopenjdk/maven-openjdk8-openj9 AS build

WORKDIR /project

# Copy project POM
COPY coffeeshop-service/pom.xml /project/coffeeshop-service/pom.xml
COPY pom.xml /project/

# Download dependencies
RUN mvn -q -f coffeeshop-service/pom.xml clean dependency:resolve

# Copy application sources
COPY coffeeshop-service/src /project/coffeeshop-service/src

# Build and package
RUN mvn -q -f coffeeshop-service/pom.xml package

## Create runtime Image
FROM open-liberty

COPY --from=build --chown=1001:0 /project/coffeeshop-service/target/coffeeshop-service-1.0-SNAPSHOT.war /opt/ol/wlp/usr/servers/defaultServer/apps
COPY --from=build --chown=1001:0 /project/coffeeshop-service/src/main/liberty/config/server.xml /opt/ol/wlp/usr/servers/defaultServer

EXPOSE 8080

# We inherit the CMD to start Liberty from the open-liberty image
