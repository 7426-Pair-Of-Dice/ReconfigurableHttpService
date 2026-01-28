# Reconfigurable Http Service

## Overview
This project is designed to quickly iterate variables, "constants," and configuration properties for a FRC robot using HTTP requests. It includes classes for handling properties and managing reconfigurable settings. 

## Project Structure
```
ReconfigurableHttpService
├── src
│   ├── main
│   │   ├── java
│   │   │   ├── frc
│   │   │   │   ├── robot
│   │   │   │   │   ├── subsystems
│   │   │   │   │   │   └── ReconfigurableConfig.java
│   │   │   └── PropertiesHttpService.java
│   │   └── resources
│   └── test
│       └── java
├── build.gradle
├── settings.gradle
└── README.md
```

## Classes
- **PropertiesHttpService**: Handles HTTP requests related to properties, including methods for retrieving and updating configuration properties.
- **ReconfigurableConfig**: Manages reconfigurable settings for the robot, providing methods for getting and setting configuration values, along with validation.

## Resources
The `src/main/resources` directory is intended for any resource files needed by the application, such as configuration files or other assets.

## Testing
Unit tests for the application will be located in the `src/test/java` directory to validate the functionality of the main application classes.

## Build
The project uses Gradle for build management. The `build.gradle` file specifies the project dependencies, plugins, and tasks needed to build the project.

## Setup Instructions
1. Clone the repository.
2. Navigate to the project directory.
3. Run `gradle build` to compile the project.
4. Use `gradle run` to start the application.

## Usage
Refer to the documentation within the code for specific usage instructions related to the `PropertiesHttpService` and `ReconfigurableConfig` classes.