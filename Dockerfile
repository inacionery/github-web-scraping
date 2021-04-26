FROM openjdk:8-jdk-alpine

MAINTAINER inacionery@gmail.com

COPY build/libs/github-web-scraping-1.0.0.jar github-web-scraping.jar

ENTRYPOINT ["java","-jar","/github-web-scraping.jar"]