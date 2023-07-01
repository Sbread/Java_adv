cd ..\java-solutions
set JAR=..\scripts\implementor.jar
set CLASS=info.kgeorgiy.ja.osipov.implementor.Implementor
set DIR=C:\JavaAdv\java-advanced\scripts
javac info\kgeorgiy\ja\osipov\implementor\Implementor.java
java -jar %JAR% %CLASS% %DIR%
java -jar %JAR% -jar %CLASS% %DIR%\ImplementorImpl.jar
