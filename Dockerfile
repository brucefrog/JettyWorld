FROM openjdk:8-jre-alpine

# add our user and group first to make sure their IDs get assigned consistently, regardless of whatever dependencies get added
# RUN groupadd -r jetson && useradd -r -g jetson jetson
RUN addgroup -S jetson && adduser -S -g jetson jetson

ENV APP_HOME /home/jetson

RUN mkdir -p "$APP_HOME"
ADD --chown=jetson bin/JettyWorld-1.0-SNAPSHOT.jar "$APP_HOME/JettyWorld-1.0.jar"

RUN mkdir -p "$APP_HOME/depend"
ADD --chown=jetson bin/dependency/ "$APP_HOME/depend"

WORKDIR "$APP_HOME"

USER jetson
EXPOSE 6800

RUN ls -lR "$APP_HOME"

CMD ["java","-cp","JettyWorld-1.0.jar:depend/*","com.jfrog.sample.Hello"]
# CMD ["ls","-R"]