FROM eclipse-temurin:17-jre
RUN addgroup spring && adduser spring --ingroup spring
USER spring:spring
COPY target/mycore-importer-cli.jar cli.jar
COPY target/mycore-importer-webapp.jar webapp.jar
ENTRYPOINT ["java","-jar"]
CMD ["webapp.jar"]