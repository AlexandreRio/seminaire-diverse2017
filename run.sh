#!/bin/bash
gradle fatJar
java -jar build/libs/fr.irisa.seminar2017-all-1.0-SNAPSHOT.jar
