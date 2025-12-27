FROM amazoncorretto:21
ENV DISCORD_TOKEN="not_set"
COPY ./build/libs/app-all.jar /opt/app/
VOLUME /opt/app/data
WORKDIR /opt/app
CMD ["java", "-showversion", "-jar", "/opt/app/app-all.jar"]