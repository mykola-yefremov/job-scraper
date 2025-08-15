FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y \
    wget \
    gnupg2 \
    software-properties-common \
    curl \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/job-scraper-0.0.1-SNAPSHOT.jar"]




