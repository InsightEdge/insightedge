#!/bin/bash
set -x

if [ $# -eq 1 ]; then
    if [ ! -e "$1" ]; then
        echo "File [$1] does not exist."
        exit 1
    else
        source $1
    fi
fi


function uniquify_timer_triggered_nightly_git_tag_name {
    if [[ "$TAG_NAME" == *NIGHTLY ]]
    then
        TAG_NAME="${TAG_NAME}-$(date +'%A')"
	    LONG_TAG_NAME="${TAG_NAME}-$(date '+%Y-%m-%d-%H-%M-%S')"
    fi
}

uniquify_timer_triggered_nightly_git_tag_name


# Get the folder from git url 
# $1 is a git url of the form git@github.com:Gigaspaces/xap-open.git
# The function will return a folder in the $WORKSPACE that match this git url (for example $WORKSPACE/xap-open)
function get_folder {
    echo -n "$WORKSPACE/$(echo -e $1 | sed 's/.*\/\(.*\)\.git/\1/')"
}


# Rename all version of each pom.xml in $1 folder to $IE_MAVEN_VERSION
function rename_poms {
    # Find current version from the pom.xml file in $1 folder.
    local version="$(grep -m1 '<version>' $1/pom.xml | sed 's/<version>\(.*\)<\/version>/\1/')"
    # Since grep return the whole line there are spaces that needed to trim.
    local trimmed_version="$(echo -e "${version}" | tr -d '[[:space:]]')"
    # Find each pom.xml under $1 and replace every $trimmed_version with $IE_MAVEN_VERSION

    find "$1" -name "pom.xml" -exec sed -i "s/$trimmed_version/$IE_MAVEN_VERSION/" \{\} \;
}

# replace all occurrences of <insightedge.version>x.y.z-SNAPSHOT</insightedge.version> with <insightedge.version>${FINAL_IE_BUILD_VERSION}</insightedge.version>
function rename_ie_version  {
    local trimmed_version="<insightedge\.version>.*<\/insightedge\.version>"
    find "$1" -name "pom.xml" -exec sed -i "s/$trimmed_version/<insightedge.version>$IE_MAVEN_VERSION<\/insightedge.version>/" \{\} \;
}

# replace all occurrences of <xap.version>x.y.z-SNAPSHOT</xap.version> with <xap.version>${XAP_RELEASE_VERSION}</xap.version>
function rename_xap_version  {
    local trimmed_version="<xap\.version>.*<\/xap\.version>"
    find "$1" -name "pom.xml" -exec sed -i "s/$trimmed_version/<xap.version>$XAP_RELEASE_VERSION<\/xap.version>/" \{\} \;
}

# Clean all nightly tags older then 7 days.
function clean_old_tags {
    local dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    if [ -x "${dir}/clean_old_nightly_tags.sh" ]
    then
       (cd "$1"; ${dir}/clean_old_nightly_tags.sh)
    fi
}

# Create a temporary branch for the pom changes commits.
# If such branch already exists delete it (is it wise ?)
# Do not push this branch.
function create_temp_branch {
    local temp_branch_name="$1"
    local git_folder="$2"
    (
	cd "$git_folder"
	git checkout "$BRANCH"
	git show-ref --verify --quiet "refs/heads/$temp_branch_name"
	if [ "$?" -eq 0 ]
	then
	    git branch -D  "$temp_branch_name"
	fi
	git checkout -b "$temp_branch_name"
    )
}

#
function clean_m2 {
    if [ "$PERFORM_FULL_M2_CLEAN" = "true" ]
    then
	rm -rf $M2/repository
    else
	rm -rf $M2/repository/org/xap      
	rm -rf $M2/repository/org/gigaspaces 
	rm -rf $M2/repository/com/gigaspaces 
	rm -rf $M2/repository/org/openspaces 
	rm -rf $M2/repository/com/gs         
    fi
}

function execute_command {
    # $1 = title
    # $2 = directory
    # $3 = command
    local cmd="$3"
    pushd "$2"
    echo "****************************************************************************************************"
    echo "$1"
    echo "Executing cmd: $cmd"
    echo "****************************************************************************************************"
    eval "$cmd"
    local r="$?"
    popd
    if [ "$r" -ne 0 ]
    then
        echo "[ERROR] Failed While installing using maven in folder: $1, command is: $cmd, exit code is: $r"
        exit "$r"
    fi


}
# Call maven install from directory $1
# In case of none zero exit code exit code stop the release
function mvn_install {
    local rep="$2"
    if [ "$rep" == "IE" ]; then
        cmd="mvn -B -Dmaven.repo.local=$M2/repository clean install -DskipTests -Pbuild-resources"
        execute_command "Installing $rep" "$1" "$cmd"
    elif [ "$rep" == "IE_Example" ]; then
        cmd="mvn -B -Dmaven.repo.local=$M2/repository test package -DskipTests"
        execute_command "Installing $rep" "$1" "$cmd"
    elif [ "$rep" == "IE_ZEPPELIN" ]; then
        cmd="./dev/change_scala_version.sh 2.11"
        execute_command "Changing scala version - $rep" "$1" "$cmd"
        cmd="mvn -B -Dmaven.repo.local=$M2/repository clean package -DskipTests -Drat.skip=true -Pspark-2.1 -Pscala-2.11 -Pbuild-distr"
        execute_command "Installing $rep" "$1" "$cmd"
    fi
}

function package_ie {
    local rep="$2"
    if [ "$rep" == "IE_PACKAGE_COMMUNITY" ]; then
        cmd="mvn -e -B -Dmaven.repo.local=$M2/repository package -Ppackage-community -Dinsightedge.full.build.version=${FINAL_IE_BUILD_VERSION} -DskipTests=true -Ddist.spark=$WORKSPACE/spark-2.1.0-bin-hadoop2.7.tgz -Ddist.xap=$XAP_OPEN_URL -Ddist.zeppelin=$WORKSPACE/insightedge-zeppelin/zeppelin-distribution/target/zeppelin.tar.gz -Ddist.examples=$WORKSPACE/insightedge-examples/target/insightedge-examples-all.zip"
        execute_command "Packaging $rep" "$1" "$cmd"
    elif [ "$rep" == "IE_PACKAGE_PREMIUM" ]; then
        cmd="mvn -e -B -Dmaven.repo.local=$M2/repository package -Ppackage-premium -Dinsightedge.full.build.version=${FINAL_IE_BUILD_VERSION} -DskipTests=true -Ddist.spark=$WORKSPACE/spark-2.1.0-bin-hadoop2.7.tgz -Ddist.xap=$XAP_PREMIUM_URL -Ddist.zeppelin=$WORKSPACE/insightedge-zeppelin/zeppelin-distribution/target/zeppelin.tar.gz -Ddist.examples=$WORKSPACE/insightedge-examples/target/insightedge-examples-all.zip"
        execute_command "Packaging $rep" "$1" "$cmd"
    fi
}



# Call maven deploy from directory $1
# It uses the target deploy:deploy to bypass the build.
# In case of none zero exit code exit code stop the release
function mvn_deploy {

    # TODO remove XAP_RELEASE_VERSION
    cmd="mvn -B -Dmaven.repo.local=$M2/repository -DskipTests deploy -Dxap.version=${XAP_RELEASE_VERSION}"
    execute_command "Maven deploy" "$1" "$cmd"
}

# Commit local changes and create a tag in dir $1.
# It assume the branch is the local temp branch,
function commit_changes {
    local folder="$1"
    local msg="Modify poms to $FINAL_IE_BUILD_VERSION in temp branch that was built on top of $BRANCH"

    pushd "$folder"
    git add -u
    git commit -m "$msg"
    git tag -f -a "$TAG_NAME" -m "$msg"
    if [ ! -z "${LONG_TAG_NAME}" ]
    then
	   git tag -f -a "$LONG_TAG_NAME" -m "$msg"
    fi

    popd
}

# Delete the temp branch $2 in folder $1
# Push the tag to origin
function delete_temp_branch {
    local folder="$1"
    local temp_branch="$2"

    pushd "$folder"

    git checkout -q "$TAG_NAME"
    git branch -D "$temp_branch"

    if [ "$OVERRIDE_EXISTING_TAG" != "true" ]
    then
	    git push origin "$TAG_NAME"
    else
	    git push -f origin "$TAG_NAME"
    fi

    if [ ! -z "${LONG_TAG_NAME}" ]
    then
	   git push origin "$LONG_TAG_NAME"
    fi

    popd
}

function exit_if_tag_exists {
    local folder="$1"

    pushd "$folder"
    git show-ref --verify --quiet "refs/tags/$TAG_NAME"
    local r="$?"
    if [ "$r" -eq 0 ]
    then
        echo "[ERROR] Tag $TAG_NAME already exists in repository $folder, you can set OVERRIDE_EXISTING_TAG=true to override this tag"
        exit 1
    fi
    popd
}


function upload_ie_zip {
# $1 = folder
# $2 = type "xap-open" , "xap-premium" , "xap-enterprise"
# $3 = "with-license" or "without-license"
    echo "uploading zip $1 $2"
    local folder="$1"
    pushd "$folder"
    local sourceZipFileName
    local targetPath
    local sourceZipFileLocation="insightedge-packager/target/"

    if [ "$2" = "ie-community" ]; then
       sourceZipFileLocation="${sourceZipFileLocation}/community/"
       zipFileName="gigaspaces-insightedge-${FINAL_IE_BUILD_VERSION}-community.zip"
       targetPath="com/gigaspaces/insightedge/${IE_VERSION}/${FINAL_IE_BUILD_VERSION}"
    elif [ "$2" = "ie-premium" ]; then
       sourceZipFileLocation="${sourceZipFileLocation}/premium/"
       zipFileName="gigaspaces-insightedge-${FINAL_IE_BUILD_VERSION}-premium.zip"
       targetPath="com/gigaspaces/insightedge/${IE_VERSION}/${FINAL_IE_BUILD_VERSION}"

    fi


    sourceZipFileLocation="${sourceZipFileLocation}/${zipFileName}"

    cmd="mvn -Dmaven.repo.local=$M2/repository com.gigaspaces:xap-build-plugin:deploy-native -Pbuild-resources -Dput.source=${sourceZipFileLocation} -Dput.target=${targetPath} -Dput.container=gigaspaces-repository-eu"

    echo "****************************************************************************************************"
    echo "uploading $2 zip"
    echo "Executing cmd: $cmd"
    echo "****************************************************************************************************"
    eval "$cmd"
    local r="$?"
    if [ "$r" -eq 1 ]
    then
        echo "[ERROR] failed to upload $2 zip, exit code:$r"
    fi
    popd
}


let step=1
function announce_step {
    local tooks=""
    if [ -z ${last_step+x} ]
    then
       let start_time=$(date +'%s')
    else
	local seconds="$(($(date +'%s') - $start_time))"
	local formatted=`date -u -d @${seconds} +"%T"`
	tooks="$last_step tooks: $formatted (hh:mm:ss)"
    fi
    last_step=$1

    if [ "" != "$tooks" ]
    then
	echo ""
	echo ""
	echo "############################################################################################################################################################"
	echo "############################################################################################################################################################"
	echo "#                                                  $tooks"
	echo "############################################################################################################################################################"
	echo "############################################################################################################################################################"
    fi
    echo ""
    echo ""
    echo ""
    echo "************************************************************************************************************************************************************"
    echo "************************************************************************************************************************************************************"
    echo "*                                                      Step [$step]: $1"
    echo "************************************************************************************************************************************************************"
    echo "************************************************************************************************************************************************************"
    (( step++ ))
    let start_time=$(date +'%s')
}


# Clone xap-open and xap.
# Clean m2 from xap related directories.
# Create temporary local git branch.
# Rename poms.
# Call maven install.
# Commit changes.
# Create tag.
# Delete the temporary local branch.
# Push the tag
# Call maven deploy.
# upload zip to s3.

function publish_ie {

    baseOutputFolder="${WORKSPACE}/insightedge/insightedge-packager/target/"
    premiumZipFileName="gigaspaces-insightedge-${FINAL_IE_BUILD_VERSION}-premium.zip"
    communityZipFileName="gigaspaces-insightedge-${FINAL_IE_BUILD_VERSION}-community.zip"

    host=${storageHost}
    echo "host=${host}"

    port=${storagePort}
    echo "port=${port}"

    user=${storageUser}
    echo "user=${user}"

    branch=${BRANCH}
    echo "branch=${BRANCH}"

    version=${IE_VERSION}
    echo "version=${IE_VERSION}"

    buildNumber=${IE_FINAL_BUILD_NUMBER}
    echo "buildNumber=${buildNumber}"

    base_path="/home/giga/backup-store/insightedge"
    build_path="${version}/${branch}/${buildNumber}"
    path_on_server="${base_path}/${build_path}"

    communityZipFilePath=${baseOutputFolder}/community/${communityZipFileName}/
    premiumZipFilePath=${baseOutputFolder}/premium/${premiumZipFileName}

    echo "check if insightedge community zip exists"
    ls -l ${communityZipFilePath}

    echo "check if insightedge premium zip exists"
    ls -l ${premiumZipFilePath}

    echo "creating dir in server - [$path_on_server]"
    ssh ${user}@${host} mkdir -p ${path_on_server}
    ssh ${user}@${host} chown -R "${user}:domain\ users" ${base_path}/${version}
    ssh ${user}@${host} chmod -R 755 ${base_path}/${version}

    echo "coping resources to: [$host] to folder in server:[${path_on_server}]"
    scp ${communityZipFilePath} ${premiumZipFilePath} ${baseOutputFolder}/*.json ${baseOutputFolder}/integration-tests-sources.zip ${baseOutputFolder}/metadata.txt ${baseOutputFolder}/newman-artifacts.zip ${user}@${host}:${path_on_server}
    ssh ${user}@${host} chmod -R 755 ${path_on_server}

    echo "starting newman submitter process..."
    java -version
    wget http://xap-test.s3.amazonaws.com/qa/newman/newman-submitter-1.0.jar -O ${WORKSPACE}/newman-submitter-1.0.jar
    sumbitter_jar=${WORKSPACE}/newman-submitter-1.0.jar
    if [ -f $sumbitter_jar ]; then
        BASE_WEB_URI=http://${host}:${port}/insightedge
        WEB_PATH_TO_BUILD=${BASE_WEB_URI}/${build_path}
        echo "web url for build resources: ${WEB_PATH_TO_BUILD}"

        export NEWMAN_HOST=${newmanHost="xap-newman"}
        export NEWMAN_PORT=${newmanPort="8443"}
        export NEWMAN_USER_NAME=${newmanUsername="root"}
        export NEWMAN_PASSWORD=${newmanPassword="root"}
        export NEWMAN_BUILD_BRANCH=${branch}
        export NEWMAN_BUILD_NUMBER=${buildNumber}
        export NEWMAN_BUILD_TAGS=${NEWMAN_TAGS}
        export NEWMAN_BUILD_TESTS_METADATA=${WEB_PATH_TO_BUILD}/ie-integration-tests.json
        export NEWMAN_BUILD_SHAS_FILE=${WEB_PATH_TO_BUILD}/metadata.txt
        export NEWMAN_BUILD_RESOURCES=${WEB_PATH_TO_BUILD}/integration-tests-sources.zip,${WEB_PATH_TO_BUILD}/${communityZipFileName},${WEB_PATH_TO_BUILD}/${premiumZipFileName},${WEB_PATH_TO_BUILD}/newman-artifacts.zip

        echo "NEWMAN_BUILD_BRANCH=${NEWMAN_BUILD_BRANCH}"
        echo "NEWMAN_BUILD_NUMBER=${NEWMAN_BUILD_NUMBER}"
        echo "NEWMAN_HOST=${NEWMAN_HOST}"
        echo "NEWMAN_USER_NAME=${NEWMAN_USER_NAME}"
        echo "NEWMAN_BUILD_TESTS_METADATA=${NEWMAN_BUILD_TESTS_METADATA}"
        echo "NEWMAN_BUILD_SHAS_FILE=${NEWMAN_BUILD_SHAS_FILE}"
        echo "NEWMAN_BUILD_RESOURCES=${NEWMAN_BUILD_RESOURCES}"
        java -cp ${sumbitter_jar} com.gigaspaces.newman.NewmanBuildSubmitter
        echo "finished newman submitter process successfully"
    else
        echo "skipping newman submitter process because the file ${sumbitter_jar} does not exists"
        exit 1
    fi


}



function release_ie {
    env
    local xap_open_url="git@github.com:xap/xap.git"
    local xap_url="git@github.com:Gigaspaces/xap-premium.git"
    local ie_url="git@github.com:InsightEdge/insightedge.git"
    local ie_exm_url="git@github.com:InsightEdge/insightedge-examples.git"
    local ie_zeppelin_url="git@github.com:InsightEdge/insightedge-zeppelin.git"

    local temp_branch_name="$BRANCH-$FINAL_IE_BUILD_VERSION"
    local ie_folder="$(get_folder $ie_url)"
    local ie_exm_folder="$(get_folder $ie_exm_url)"
    local ie_zeppelin_folder="$(get_folder $ie_zeppelin_url)"


    clean_old_tags "$ie_folder"
    clean_old_tags "$ie_exm_folder"
    clean_old_tags "$ie_zeppelin_folder"



    if [ "$OVERRIDE_EXISTING_TAG" != "true" ] 
    then
        announce_step "delete tag $TAG in ie"
	    exit_if_tag_exists "$ie_folder"
	    announce_step "delete tag $TAG in ie example"
	    exit_if_tag_exists "$ie_exm_folder"
	    announce_step "delete tag $TAG in ie zeppelin"
	    exit_if_tag_exists "$ie_zeppelin_folder"
    fi

    announce_step "clean m2"
    clean_m2


    announce_step "create temporary local branch $temp_branch_name in ie"
    create_temp_branch "$temp_branch_name" "$ie_folder"

    announce_step "create temporary local branch $temp_branch_name in ie example"
    create_temp_branch "$temp_branch_name" "$ie_exm_folder"

    announce_step "create temporary local branch $temp_branch_name in ie zeppelin"
    create_temp_branch "$temp_branch_name" "$ie_zeppelin_folder"

    announce_step "rename version in poms [ie]"
    rename_poms "$ie_folder"
    announce_step "rename xap version in poms [ie]"
    rename_xap_version "$ie_folder"

    announce_step "rename version in poms [ie example]"
    rename_poms "$ie_exm_folder"

    announce_step "rename ie version in poms [ie zeppelin]"
    rename_ie_version "$ie_zeppelin_folder"
    announce_step "rename version in poms [ie zeppelin]"
    rename_poms "$ie_zeppelin_folder"


    if [ ! -f "$WORKSPACE/spark-2.1.0-bin-hadoop2.7.tgz" ]
    then
        announce_step "Downloading spark distribution"
        wget https://d3kbcqa49mib13.cloudfront.net/spark-2.1.0-bin-hadoop2.7.tgz -P ${WORKSPACE}
        echo "Finished downloading spark"
    else
        echo "Found Spark distribution, download is skipped"
    fi


    announce_step "executing maven install on ie"
    mvn_install "$ie_folder" "IE"
    echo "Done installing ie"

    announce_step "executing maven install on ie example"
    mvn_install "$ie_exm_folder" "IE_Example"
    echo "Done installing ie example"


    announce_step "executing maven install on ie zeppelin"
    mvn_install "$ie_zeppelin_folder" "IE_ZEPPELIN"
    echo "Done installing ie zeppelin"

    announce_step "package ie community package"
    package_ie "$ie_folder" "IE_PACKAGE_COMMUNITY"
    echo "Done package ie community"

    announce_step "package ie premium package"
    package_ie "$ie_folder" "IE_PACKAGE_PREMIUM"
    echo "Done package ie premium"

    announce_step "committing changes in ie"
    commit_changes "$ie_folder"

    announce_step "committing changes in ie examples"
    commit_changes "$ie_exm_folder"

    announce_step "committing changes in ie zeppelin"
    commit_changes "$ie_zeppelin_folder"

    announce_step "delete temp branch $temp_branch_name in ie"
    delete_temp_branch "$ie_folder" "$temp_branch_name"

    announce_step "delete temp branch $temp_branch_name in ie examples"
    delete_temp_branch "$ie_exm_folder" "$temp_branch_name"

    announce_step "delete temp branch $temp_branch_name in ie zeppelin"
    delete_temp_branch "$ie_zeppelin_folder" "$temp_branch_name"

    announce_step "publish ie to hercules and newman"
    publish_ie

    announce_step "uploading ie-community zip"
    upload_ie_zip "$ie_folder" "ie-community"

    announce_step "uploading ie-premium zip"
    upload_ie_zip "$ie_folder" "ie-premium"


	announce_step "deploying IE maven artifacts"
	mvn_deploy "$ie_folder"

    announce_step "DONE !"

}


release_ie