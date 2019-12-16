FROM adoptopenjdk/openjdk8-openj9:x86_64-ubi-minimal-jdk8u232-b09_openj9-0.17.0 AS build
WORKDIR /

# Install maven
RUN curl -LO https://www-us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz && tar -xzf apache-maven-3.6.3-bin.tar.gz && export PATH=/apache-maven-3.6.3/bin:$PATH && mvn -version
ENV PATH=/apache-maven-3.6.3/bin:$PATH

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

# Start and stop Liberty to create the shared class cache
# To capture JVM start options, add: -verbose:gc -Xverbosegclog:/tmp/gc.log.%p
ENV JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Xtune:virtualized"
RUN /opt/ol/wlp/bin/server start defaultServer && sleep 5 && /opt/ol/wlp/bin/server stop defaultServer && (java -Xshareclasses:name=liberty-%u,nonfatal,cacheDir=/opt/ol/wlp/output/.classCache,printStats || echo "OK")

EXPOSE 8080

# We inherit the CMD to start Liberty from the open-liberty image
