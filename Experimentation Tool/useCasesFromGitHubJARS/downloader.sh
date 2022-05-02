#!/bin/bash
sumSuccessfullTests=0
sumFailedTests=0
sumLinks=0
sumLinksOver100SuccessfullTests=0

for directory in ../useCasesFromGitHubJARS/*; do

    if [[ -d $directory ]]; then

        linkFile=$directory"/repositoryLink.txt"
        link=$(<$linkFile)
        
        git clone $link
        
    fi
    
done
