FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/tutor-auth-0.0.1-SNAPSHOT.jar tutor-auth.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/tutor-auth.jar"]

#серверный вариант
#FROM eclipse-temurin:21-jdk AS builder
#WORKDIR /app
#COPY . .
#RUN ./gradlew clean build -x test
#
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#COPY   --from=builder /app/build/libs/tutor-auth-0.0.1-SNAPSHOT.jar tutor-auth.jar
#EXPOSE 8081
#ENTRYPOINT ["java","-jar","/app/tutor-auth.jar"]
