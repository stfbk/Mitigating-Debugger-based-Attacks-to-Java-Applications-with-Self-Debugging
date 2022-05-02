 #!/bin/bash
sumSuccessfullTests=0
sumFailedTests=0
sumLinks=0
sumLinksOver100SuccessfullTests=0

for directory in *; do

    if [[ -d $directory ]]; then

        cd $directory
    
        rm '../../useCasesFromGitHubJARS/'$directory'/linesOfCodeMain.txt'
        rm '../../useCasesFromGitHubJARS/'$directory'/linesOfCodeTest.txt'
        rm '../../useCasesFromGitHubJARS/'$directory'/javaClassesMain.txt'
        rm '../../useCasesFromGitHubJARS/'$directory'/javaClassesTest.txt'
        
        find . -type f -path './src/main/java/*.java' | xargs wc -l | sort -nr > '../../useCasesFromGitHubJARS/'$directory'/linesOfCodeMain.txt'
        find . -type f -path './src/test/java/*.java' | xargs wc -l | sort -nr > '../../useCasesFromGitHubJARS/'$directory'/linesOfCodeTest.txt'
        
        find . -type f -path './src/main/java/*.java' | wc -l > '../../useCasesFromGitHubJARS/'$directory'/javaClassesMain.txt'
        find . -type f -path './src/test/java/*.java' | wc -l > '../../useCasesFromGitHubJARS/'$directory'/javaClassesTest.txt'
        
        cd ..
    
    fi
    
done
