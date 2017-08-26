FROM openjdk:8-jre-alpine

COPY target/applicationinterrupts-*.jar /usr/app/examples.jar
WORKDIR /usr/app

CMD ["java", "-jar", "examples.jar"]
