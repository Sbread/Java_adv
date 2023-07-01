SET link="https://docs.oracle.com/en/java/javase/19/docs/api/"
set SRC_PATH="../java-advanced-2023/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor"
set IMPL_PATH="java-solutions/info/kgeorgiy/ja/osipov/implementor"
set artifacts="../java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar"

cd..

javadoc -link %link% ^
	-cp %artifacts% ^
	-private ^
    %IMPL_PATH%/Implementor.java ^
    %IMPL_PATH%/CustomMethod.java ^
    %IMPL_PATH%/DeleteFileVisitor.java ^
	%SRC_PATH%/ImplerException.java ^
	%SRC_PATH%/JarImpler.java ^
	%SRC_PATH%/Impler.java ^
	-d /JavaAdv/java-advanced/javadoc
