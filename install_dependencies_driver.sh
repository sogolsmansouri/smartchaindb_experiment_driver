
#Install SDK to install Gradle, run the following commands: 
curl -s "https://get.sdkman.io" | bash
source "/home/nkorchi/.sdkman/bin/sdkman-init.sh" 
sdk help
sdk install gradle 3.0

#Install JAVA 8
sudo apt-get install openjdk-8-jdk
sudo update-alternatives --config java
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

