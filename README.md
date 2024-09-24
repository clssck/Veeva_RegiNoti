# Registration Lifecycle Notification

## Overview
This project implements a Record Trigger for the 'registration__rim' object in Veeva Vault. It sends a notification to the responsible person when the 'state__v' field of a registration is updated.

## Features
- Monitors changes in the 'state__v' field of registration records
- Sends notifications to the responsible person when the state changes
- Includes detailed state descriptions in the notification message

## Project Structure
- `src/main/java/com/veeva/vault/custom/triggers/RegistrationLifecycleNotification.java`: Main trigger implementation
- `plugin_settings_file.json`: Plugin settings for deployment
- `vapil_settings_file.json`: VAPIL settings for authentication
- `pom.xml`: Maven project configuration
- `vaultpackage.xml`: Vault package configuration

## Dependencies
- Veeva Vault SDK (version 24.2.0)
- Veeva Vault SDK Debugger (version 24.2.0)

## Configuration
1. Update `plugin_settings_file.json` with appropriate package details
2. Configure `vapil_settings_file.json` with your Vault credentials
3. Ensure `pom.xml` has the correct Vault SDK version and plugin configurations

## Building and Deployment
Use Maven to build the project:
```
mvn vaultjavasdk:clean
mvn vaultjavasdk:package
mvn vaultjavasdk:validate
```

![example1](/lilregi1.png "example 1")
![example2](/lilregi2.png "example 2")
