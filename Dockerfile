FROM openjdk:8-jre-alpine

# add our user and group first to make sure their IDs get assigned consistently, regardless of whatever dependencies get added
# ### Debian base for openjdk:8-jre
# RUN groupadd -r jetson && useradd -r -g jetson jetson
# ### Alpine base for openjdk:8-jre-alpine
RUN addgroup -S jetson && adduser -S -g jetson jetson

ENV APP_HOME /home/jetson
ENV BUILD_JAR=JettyWorld-1.0-SNAPSHOT.jar
ENV APP_JAR=main.jar

RUN mkdir -p "$APP_HOME"
COPY --chown=jetson bin/$BUILD_JAR "$APP_HOME/$APP_JAR"

RUN mkdir -p "$APP_HOME/depend"
COPY --chown=jetson bin/dependency/ "$APP_HOME/depend"

WORKDIR "$APP_HOME"

USER jetson
EXPOSE 6800

# uncomment for debugging
# RUN ls -lR "$APP_HOME"

ENV CLASSPATH="${APP_JAR}:depend/*"
ENV MAIN_CLASS=com.jfrog.sample.Hello

CMD ["sh","-c","java -cp ${CLASSPATH} ${MAIN_CLASS}"]