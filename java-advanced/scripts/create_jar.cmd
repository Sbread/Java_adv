set main_path=info\kgeorgiy\ja\osipov\implementor
set artifacts=java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar
cd ..\..

javac --class-path %artifacts% java-advanced/java-solutions/%main_path%/*.java

cd java-advanced/java-solutions

jar --create --manifest=..\scripts\MANIFEST.MF --file=..\scripts\implementor.jar %main_path%\*.java %main_path%\*.class

del %main_path%\*.class

cd ..\scripts

