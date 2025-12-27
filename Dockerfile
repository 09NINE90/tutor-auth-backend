FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/tutor-auth-0.0.1-SNAPSHOT.jar tutor-auth.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/tutor-auth.jar"]
