# Use the official OpenJDK 21 image
FROM openjdk:21

# Set the working directory to /app
WORKDIR /app

# Copy the JAR file from the target directory
COPY target/helloCash-0.0.1-SNAPSHOT.jar /app/

# Expose the port
EXPOSE 8085

# Run the JAR file when the container starts
ENTRYPOINT ["java", "-jar", "helloCash-0.0.1-SNAPSHOT.jar"]