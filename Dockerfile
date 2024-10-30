FROM eclipse-temurin:17-jre
RUN groupadd -r spring -g 501 && \
    useradd -d /home/spring -u 501 -m -s /bin/bash -g spring spring  && \
    apt-get update && \
    apt-get install -y gosu && \
    rm -rf /var/lib/apt/lists/*
USER spring:spring
WORKDIR /home/spring/
COPY docker-entrypoint.sh /home/spring/entrypoint.sh
COPY target/mycore-importer-cli.jar cli.jar
COPY target/mycore-importer-webapp.jar webapp.jar
USER root:root
ENTRYPOINT ["bash", "entrypoint.sh"]
CMD ["java -jar webapp.jar"]