#!/bin/bash

oblive=oblive-1.0.12-release.jar

for directory in useCasesFromGitHubJARS/*; do

    if [[ -d $directory ]]; then

        echo "starting tool on directory:" "$directory";

        jarFileCode=''
        jarFileTest=''

        for jarFile in $directory/*; do
            if [[ $jarFile == *"-tests.jar"* ]]; then
                jarFileTest=$jarFile
            elif [[ $jarFile == *".jar"* ]]; then
                jarFileCode=$jarFile
            fi
        done
        cmd="java -Dlog_file_name=wrapperOutput/$directory.log -jar ./ExperimentationTool.jar -j $jarFileCode -t $jarFileTest -p antidebugtime -o ./wrapperOutput/ -h /usr/lib/jvm/java-11-openjdk-amd64 -n 100 -r ./lib/annotator.jar -b ./oblive_versions/$oblive -u ./lib/junit-platform-console-standalone-1.6.2.jar -a ./lib/org.jacoco.agent-0.8.5-runtime.jar -c ./lib/org.jacoco.cli-0.8.5-nodeps.jar -z 10"

        echo "found two files, code:" $jarFileCode "and tests" $jarFileTest
        echo "launching command:" $cmd

        $cmd
    fi

done
