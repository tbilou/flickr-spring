FROM openjdk:8-jre

EXPOSE 8080

ENV JAVA_OPTS "-Xms256m -Xmx512m"

ADD maven/flickr-spring.jar /opt/flickr-spring/
WORKDIR /opt/flickr-spring/

CMD java $JAVA_OPTS -jar flickr-spring.jar