chmod +x gradlew
./gradlew distZip
cp ../laboratory-cli/build/distributions/laboratory-cli-jvm.zip laboratory-cli-jvm.zip
sudo ./install.sh