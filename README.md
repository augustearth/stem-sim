# STEM-SIM
Multi-scale simulation of neoplasm formation.


## Requirements

### Ant
Ant is an open source software building tool available at ant.apache.org.

### Java 1.5
StemSim is pure Java and requires Java 1.5 or later.  Java is available at
java.sun.com.



## Building StemSim
To build StemSim, cd into the StemSim directory and type: ant.



## Running a StemSim Simulation
1. Modify the run.properties file as needed
    - the default run.properties is set up to run a default simulation
    - review each parameter and change as needed

2. To run the simulation: 
    - cd StemSim
    - ./bin/sim run.properties
    
StemSim will create a directory named after the time the simulation was kicked
off.  All files created by the simulation will be found in this directory.



## Computing Statistics
StemSim cancer statistics are computed after the runs have completed from the XML simulation files.  To generate the statistics:

`./bin/stat <sim xml directory>`

The stat command will create files with names based on the XML directory.
