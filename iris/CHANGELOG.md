# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

# Section Groups
* **Added** - for new features
* **Changed** - for changes in existing functionality
* **Deprecated** - for once-stable features removed in upcoming releases
* **Removed** - for deprecated features removed in this release
* **Fixed** - for any bug fixes
* **Security** - to invite users to upgrade in case of vulnerabilities
* **Unreleased** - shows changes as they occur - allows users to anticipate new functionality.

***

# Release History 
#### (For Details See Version Changes Below)
* [v1.1] - Configuration from Properties File
* [v1.0] - Drag-n-Drop of Fingerprints; Bubbles (terms), Contexts; Copy/Paste JSON; Freehand Expression Syntax
* [v0.9] - Pre-production

***

## Unreleased [v1.2-SNAPSHOT]
#### Removed
#### Added
#### Changed  
#### Fixed 

***

## [v1.1]  Main Feature: Configuration from Properties File
#### Removed
#### Added
* This CHANGLOG
* New 'ExtendedClient' code module and repository containing Endpoint abstraction to handle clients specialized for 'position lookups'
* RetianPropertyConfigurationTest in ExtendedClient repo - contains all unit tests for new Properties classes.
* OrderedProperties modified 3rd party class for ability to print out properties in 'encounter' order.
* PrefixedProperties - custom subclass of OrderedProperties to handle manipulation of properties by named groups.
* Application Menubar
* Ability to 'reset' configurations. (no more deleting the api key file) Allows ability to restate configuration to use upon next start up.
* Ability to print out example properties file to the user's home directory
* New "Load Properties File" button on the initial Overlay home screen and feature for selecting properties file to load from.
* API key loading method still remains the same. Properties file feature can not exist in tandem with previous method.
* System 'remembers' (stores in preferences) the file path of the previous loaded properties file and will auto-load it upon subsequent start ups so 
the don't have to be re-specified on every start up.
* Endpoint URL's are now explicitly and extensively checked for connection viability for each URL during startup to guarantee configuration is "sane".
* Added new deployment installer image and executable jar.
* Link to newly updated "User Guide"
#### Changed  
* README.md landing page to reflect new deployment version number.
* Links to new installer versions on README.md
#### Fixed
