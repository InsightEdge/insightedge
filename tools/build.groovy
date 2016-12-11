sh wget $1 gigaspaces-xap.zip
sh jar xf gigaspaces-xap.zip
sh chmod 775 gigaspaces-xap/tools/maven/installmavenrep.sh
sh pushd gigaspaces-xap/tools/maven
sh . installmavenrep.sh
sh popd


sh "mvn clean install"