FROM openjdk:15
ADD target/replicated-log-master-0.0.1-SNAPSHOT.jar replicated-log-master-0.0.1-SNAPSHOT.jar
EXPOSE 8090
ENV ACTIVE_PROFILE=dev
ENTRYPOINT ["java","-Djava.security.egd=f-ile:/dev/./urandom","-jar","replicated-log-master-0.0.1-SNAPSHOT.jar"]