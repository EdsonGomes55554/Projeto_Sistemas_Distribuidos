set ZK="C:\Users\rafaw\Downloads\zookeeper-3.4.14\zookeeper-3.4.14"
set CP_ZK=.;%ZK%\zookeeper-3.4.14.jar;%ZK%\lib\slf4j-log4j12-1.7.25.jar;%ZK%\lib\slf4j-api-1.7.25.jar;%ZK%\lib\log4j-1.2.17.jar
javac -cp %CP_ZK% pacote/*.java
@echo ***** Projeto
java -cp %CP_ZK% -Dlog4j.configuration=file:%ZK%\conf\log4j.properties pacote/Main localhost
