#Install Docker
MYUSER=`whoami`
sudo apt-get update
sudo apt-get install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get -y install docker-ce docker-ce-cli containerd.io
sudo adduser $MYUSER docker

#install Stardog for only 1 driver if you use more than 1
sudo docker pull stardog/stardog:latest

#create directory ~/stardog-home and put the license key inside this folder. 
#stardog is needed to get manufacturing ontologies for the experiments
#then run
#sudo docker run -it -v ~/stardog-home/:/var/opt/stardog -p 5820:5820 stardog/stardog
#sudo docker run --name=stardog-studio -p 8888:8080 -d stardog/stardog-studio:current
#sudo chmod 777 ~/stardog-home



#Install SDK to install Gradle, run the following commands: 
curl -s "https://get.sdkman.io" | bash
source "/home/nkorchi/.sdkman/bin/sdkman-init.sh" 
sdk help
sdk install gradle 3.0

#Install JAVA 8
sudo apt-get install openjdk-8-jdk
sudo update-alternatives --config java
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

