#!/bin/bash

#export JAVA_HOME="/cygdrive/c/Progra~1/AdoptOpenJDK/jdk-8.0.232.09-openj9/"
#$KAFKA_PATH/../kafka-consumer-groups.sh --bootstrap-server localhost:32100 --describe --group baristas

kafka-consumer-groups.sh --bootstrap-server 192.168.39.46:32100 --describe  --group baristas

while true; do
  sleep 5
  kafka-consumer-groups.sh --bootstrap-server 192.168.39.46:32100 --describe  --group baristas &
  #echo $OUTPUT | head -n -5
  #echo $OUTPUT | tail -n 5 | sort -n
done
