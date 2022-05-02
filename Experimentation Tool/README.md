# Mitigating Debugger-based Attacks to Java Applications with Self-Debugging | Experimental Tool

## Introduction

This folder contains the experimental framework automating debugging tasks for the assessment of anti-debugging protections. Please open the framework as an IntelliJ Java protect for more details. 


## How to Run

> Requirements: Java 11

To run the experimental framework and replicate the results shown in the paper, please clone the repository and run in a terminal the wrapper `wrapperADSelf.sh`, `wrapperADTime.sh` or `wrapperNative.sh` file, depending on the protection to test (i.e., either ADSelf, ADTime or Native). Please update the `/usr/lib/jvm/java-11-openjdk-amd64` path in the wrapper files according to the configuration of your system. Please see the [help](./help.txt) file for more details.