 
#!/bin/bash
for directory in *; do
    echo "starting analysis on directory:" "$directory";

    jarFileCode=''
    jarFileTest=''

    for jarFile in $directory/*; do
        if [[ $jarFile == *"-tests.jar"* ]]; then
            jarFileTest=$jarFile
        elif [[ $jarFile == *".jar"* ]]; then
            jarFileCode=$jarFile
        fi
    done

    echo "found two files, code:" $jarFileCode "and tests" $jarFileTest
    echo "executing command:" "java -jar ../ExperimentationTool/lib/junit-platform-console-standalone-1.6.2.jar -cp $jarFileCode:$jarFileTest --scan-classpath"
    java -jar ../ExperimentationTool/lib/junit-platform-console-standalone-1.6.2.jar -cp $jarFileCode:$jarFileTest --scan-classpath
done
