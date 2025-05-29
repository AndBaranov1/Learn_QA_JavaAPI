FROM maven:3.9.9
WORKDIR /tests
COPY . .
CMD mvn clean test