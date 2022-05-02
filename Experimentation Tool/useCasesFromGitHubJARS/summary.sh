#!/bin/bash
sumSuccessfullTests=0
sumFailedTests=0
sumLinks=0
sumLinksOver100SuccessfullTests=0

sumLinesOfCodeMain=0
sumLinesOfCodeTest=0
sumClassesMain=0
sumClassesTest=0




for directory in *; do

    if [[ -d $directory ]]; then

        linkFile=$directory"/repositoryLink.txt"
        link=$(<$linkFile)
        sumLinks=$((sumLinks+1))
        
        
        
        successfullTestFile=$directory"/numberOfSuccessfulTests.txt"
        successfullTestNumber=$(<$successfullTestFile)
        sumSuccessfullTests=$((sumSuccessfullTests+successfullTestNumber))
        
        failedTestFile=$directory"/numberOfFailedTests.txt"
        failedTestNumber=$(<$failedTestFile)
        sumFailedTests=$((sumFailedTests+failedTestNumber))
        
        sumTestNumber=$((successfullTestNumber+failedTestNumber))
        
        
        
        starFile=$directory"/starts.txt"
        starNumber=$(<$starFile)
        
        
        
        javaClassesMainFile=$directory"/javaClassesMain.txt"
        javaClassesMainNumber=$(<$javaClassesMainFile)
        sumClassesMain=$((sumClassesMain+javaClassesMainNumber))
        
        javaClassesTestFile=$directory"/javaClassesTest.txt"
        javaClassesTestNumber=$(<$javaClassesTestFile)
        sumClassesTest=$((sumClassesTest+javaClassesTestNumber))
        

        linesOfCodeMain=$(head -n 1 $directory"/linesOfCodeMain.txt")
        linesOfCodeMain=$(echo $linesOfCodeMain | cut -d' ' -f1)
        sumLinesOfCodeMain=$((sumLinesOfCodeMain+linesOfCodeMain))
        
        linesOfCodeTest=$(head -n 1 $directory"/linesOfCodeTest.txt")
        linesOfCodeTest=$(echo $linesOfCodeTest | cut -d' ' -f1)
        sumLinesOfCodeTest=$((sumLinesOfCodeTest+linesOfCodeTest))
          
          
        
        sumClassesMain=$((sumClassesMain+javaClassesMainNumber))
        
        
        
        echo $link "("$starNumber" stars):" $javaClassesMainNumber " classes ("$linesOfCodeMain "lines of code), " $javaClassesTestNumber " test classes ("$linesOfCodeTest "lines of code), " $sumTestNumber "tests ("$successfullTestNumber "successful," $failedTestNumber "failed)"
        

        if (($successfullTestNumber>100)); then
            sumLinksOver100SuccessfullTests=$((sumLinksOver100SuccessfullTests+1))
        fi

        
        
    fi
    
done


echo "In total, there are" $sumLinks "use cases with" $((sumSuccessfullTests+sumFailedTests)) "tests ("$sumSuccessfullTests "successful," $sumFailedTests "failed)"
echo "There are" $sumLinksOver100SuccessfullTests "use cases with over 100 successful tests"

