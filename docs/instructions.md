#instructons:

##1. In the NeuroID module, I need to improve coverage. Can you build unit tests in the NeuroID module for the classes in the com.neuroid.tracker package and put the test in the test in the NeuroID module. 

##Notes: 
Tests for some methods already exist in the NeuroID test directory. 

Please scan the jacoco coverage file for current coverage. The files in NeuroID/build/reports/jacocoTestReport/jacococ.xml can help you. 

In your investigations, i would like to break up the unit test work into addressing individual classes. Start with the utilities and then continue to the callbacks and services and finally the NeuroID class. Do not use Roboelectric in your unit tests. I would like the have all the tests run without roboelectric which might require some of the System static calls to be wrapped first. Please let me know if you have any questions. Please break up the phases into indiviual classes so that the PRs will be small and easy to review. 


