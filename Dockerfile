FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY build/libs/tutor-auth-1.0.0.jar tutor-auth.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","-Duser.timezone=Asia/Yekaterinburg","/app/tutor-auth.jar"]