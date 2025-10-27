# Use OpenJDK 21 as the base image
FROM eclipse-temurin:21-jdk

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY .mvn/ .mvn/
COPY pom.xml pom.xml

# Make the Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Run the application
ENTRYPOINT ["java", "-jar", "target/Mafia-0.0.1-SNAPSHOT.jar"]
