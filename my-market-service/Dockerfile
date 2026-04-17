FROM bellsoft/liberica-runtime-container:jdk-21-musl

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]