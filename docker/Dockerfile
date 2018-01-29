FROM openjdk:8-jre-alpine

# add our user and group first to make sure their IDs get assigned consistently, regardless of whatever dependencies get added
# ### Debian base for openjdk:8-jre
# RUN groupadd -r jetson && useradd -r -g jetson jetson
# ### Alpine base for openjdk:8-jre-alpine
RUN addgroup -S jetson && adduser -S -g jetson jetson

ENV APP_HOME /home/jetson

RUN mkdir -p "$APP_HOME/app"

COPY --chown=jetson bin/dependency/ "$APP_HOME/app"

WORKDIR "$APP_HOME" 

USER jetson
EXPOSE 6800

# uncomment for debugging
# RUN ls -lR "$APP_HOME"

ENV CLASSPATH="app:app/*"
ENV MAIN_CLASS=com.jfrog.sample.Hello

CMD ["sh","-c","java -cp ${CLASSPATH} ${MAIN_CLASS}"]
