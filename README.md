# mkm2csv

This small programm can place a api call to MKM to download your stock as CSV file.

The class that makes the call M11DedicatedApp is taken from the official MKM website: https://www.mkmapi.eu/ws/documentation/API:Auth_java

[Get the executable JAR file here](https://github.com/broxp/mkm2csv/raw/master/desktop/target/mkm2csv-1.0-SNAPSHOT-jar-with-dependencies.jar)

![example](screenshot.png "Example")

## if java is not installed

This program can also be run if java is not installed, but requires Java Portable extracted on your computer:

Download jPortable_8_Update_131.paf.exe from 

https://portableapps.com/apps/utilities/java_portable

Then open a console, navigate to the extracted portable apps folder, into bin and start the jar file like this:

>java -jar "C:\Users\%USERNAME%\Downloads\mkm2csv-1.0-SNAPSHOT-jar-with-dependencies.jar"
