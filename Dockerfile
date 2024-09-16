FROM eclipse-temurin:17-jre
RUN groupadd -r spring -g 501 && \
    useradd -d /home/spring -u 501 -m -s /bin/bash -g spring spring
USER spring:spring
WORKDIR /home/spring/
COPY target/mycore-importer-cli.jar cli.jar
COPY target/mycore-importer-webapp.jar webapp.jar
ENTRYPOINT ["java","-jar"]
CMD ["webapp.jar"]