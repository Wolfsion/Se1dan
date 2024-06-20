# Docker 镜像构建
# @author <a href="https://github.com/Wolfsion/">苏云</a>
# @from <a href="https://www.zhihu.com/people/dla-44-95">苏云不知云</a>
FROM maven:3.8.1-jdk-8-slim as builder

# Copy local code to the container image.
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
CMD ["java","-jar","/app/target/se1dan-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]