FROM openjdk:8-jre

# add our user and group first to make sure their IDs get assigned consistently, regardless of whatever dependencies get added
RUN groupadd -r jetson && useradd -r -g jetson jetson

ENV APP_HOME /home/jetson

RUN mkdir -p "$APP_HOME"
ADD bin/*.jar "$APP_HOME"

RUN mkdir -p "$APP_HOME/depend"
ADD bin/dependency/ "$APP_HOME/depend"

WORKDIR "$APP_HOME"

USER jetson
EXPOSE 6800

CMD ["java","-cp","JettyWorld-0.1-SNAPSHOT.jar:depend/*","com.jfrog.sample.Hello"]
# CMD ["ls","-R"]