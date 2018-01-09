#!/bin/bash
set -x


if [ $# -lt 1 ]; then
    echo "Usage: $0 setenv.sh"
    exit 1
else
    if [ ! -e "$1" ]; then
        echo "File [$1] does not exist."
        exit 1
    else
        source $1
    fi
fi



# Get the folder from git url
# $1 is a git url of the form git@github.com:Gigaspaces/xap-open.git
# The function will return a folder in the $WORKSPACE that match this git url (for example $WORKSPACE/xap-open)
function get_folder {
    echo -n "$WORKSPACE/$(echo -e $1 | sed 's/.*\/\(.*\)\.git/\1/')"
}



ie_url="git@github.com:InsightEdge/insightedge.git"
ie_exm_url="git@github.com:InsightEdge/insightedge-examples.git"
ie_zeppelin_url="git@github.com:InsightEdge/insightedge-zeppelin.git"
ie_folder="$(get_folder $ie_url)"

ie_exm_folder="$(get_folder $ie_exm_url)"
ie_zeppelin_folder="$(get_folder $ie_zeppelin_url)"

export FINAL_IE_BUILD_VERSION="$IE_VERSION-$MILESTONE-b$FINAL_BUILD_NUMBER"



# Rename all version of each pom.xml in $1 folder to $FINAL_MAVEN_VERSION
function rename_poms {
    # Find current version from the pom.xml file in $1 folder.
    local version="$(grep -m1 '<version>' $1/pom.xml | sed 's/<version>\(.*\)<\/version>/\1/')"
    # Since grep return the whole line there are spaces that needed to trim.
    local trimmed_version="$(echo -e "${version}" | tr -d '[[:space:]]')"
    # Find each pom.xml under $1 and replace every $trimmed_version with $FINAL_MAVEN_VERSION

    find "$1" -name "pom.xml" -exec sed -i "s/$trimmed_version/$FINAL_MAVEN_VERSION/" \{\} \;
}

# Rename all version of each build.sbt in $1 folder to $FINAL_MAVEN_VERSION
function rename_sbt {
    # Find current version from the build.sbt file in $1 folder.
    local version="$(grep -m1 'insightEdgeVersion' $1/build.sbt | sed 's/.*"\(.*\)".*/\1/')"
    # Since grep return the whole line there are spaces that needed to trim.
    local trimmed_version="$(echo -e "${version}" | tr -d '[[:space:]]')"
    # Find each build.sbt under $1 and replace every $trimmed_version with $FINAL_MAVEN_VERSION

    if [ "$trimmed_version" == "" ]; then
        echo "Unable to find insightEdgeVersion variable in build.sbt"
        exit 1
    fi

    find "$1" -name "build.sbt" -exec sed -i "s/$trimmed_version/$FINAL_MAVEN_VERSION/" \{\} \;
}

# replace all occurrences of <insightedge.version>x.y.z-SNAPSHOT</insightedge.version> with <insightedge.version>${FINAL_IE_BUILD_VERSION}</insightedge.version>
function rename_ie_version  {
    local trimmed_version="<insightedge\.version>.*<\/insightedge\.version>"
    find "$1" -name "pom.xml" -exec sed -i "s/$trimmed_version/<insightedge.version>$FINAL_MAVEN_VERSION<\/insightedge.version>/" \{\} \;
}

# replace all occurrences of <xap.version>x.y.z-SNAPSHOT</xap.version> with <xap.version>${FINAL_MAVEN_VERSION}</xap.version>
function rename_xap_version  {
    local trimmed_version="<xap\.version>.*<\/xap\.version>"
    find "$1" -name "pom.xml" -exec sed -i "s/$trimmed_version/<xap.version>$FINAL_MAVEN_VERSION<\/xap.version>/" \{\} \;
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
function mvn_install_cont {
    local rep="$2"
    if [ "$rep" == "IE" ]; then
        cmd="mvn -B -T 1C -Dmaven.repo.local=$M2/repository clean install -Pbuild-resources"
        execute_command "Installing $rep" "$1" "$cmd"
    elif [ "$rep" == "IE_Example" ]; then
        cmd="mvn -B -T 1C -Dmaven.repo.local=$M2/repository clean install"
        execute_command "Installing $rep" "$1" "$cmd"
    elif [ "$rep" == "IE_ZEPPELIN" ]; then
        cmd="./dev/change_scala_version.sh 2.11"
        execute_command "Changing scala version - $rep" "$1" "$cmd"
        cmd="mvn -B -T 1C -Dmaven.repo.local=$M2/repository clean install -DskipTests -Drat.skip=true -Pspark-2.2 -Dspark.version=2.2.0 -Pscala-2.11 -Pbuild-distr"
        execute_command "Installing $rep" "$1" "$cmd"
    fi
}

function mvn_install_release {
    local rep="$2"
    if [ "$rep" == "IE" ]; then
        cmd="mvn -B -T 1C -Dmaven.repo.local=$M2/repository clean install -DskipTests -Pbuild-resources"
        execute_command "Installing $rep" "$1" "$cmd"
    elif [ "$rep" == "IE_Example" ]; then
        cmd="mvn -B -T 1C -Dmaven.repo.local=$M2/repository clean install -DskipTests"
        execute_command "Installing $rep" "$1" "$cmd"
    elif [ "$rep" == "IE_ZEPPELIN" ]; then
        cmd="./dev/change_scala_version.sh 2.11"
        execute_command "Changing scala version - $rep" "$1" "$cmd"
        cmd="mvn -B -T 1C -Dmaven.repo.local=$M2/repository clean install -DskipTests -Drat.skip=true -Pspark-2.2 -Dspark.version=2.2.0 -Pscala-2.11 -Pbuild-distr"
        execute_command "Installing $rep" "$1" "$cmd"
    fi
}

function package_ie {
    local rep="$2"

    local ie_sha=${IE_SHA}
    if [ ! -z "${LONG_TAG_NAME}" ]
    then
	    ie_sha="${LONG_TAG_NAME}"
    fi

    local package_args="-Dlast.commit.hash=${ie_sha} -Dinsightedge.version=${IE_VERSION} -Dinsightedge.build.number=${FINAL_BUILD_NUMBER} -Dinsightedge.milestone=${MILESTONE} -DskipTests=true -Ddist.spark=$WORKSPACE/spark-2.2.0-bin-hadoop2.7.tgz -Ddist.zeppelin=$WORKSPACE/insightedge-zeppelin/zeppelin-distribution/target/zeppelin.tar.gz -Ddist.examples.target=$WORKSPACE/insightedge-examples/target"

    if [ "$rep" == "IE_PACKAGE_OPEN" ]; then
        echo In open @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        echo In open @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        cmd="mvn -e -B -Dmaven.repo.local=$M2/repository package -pl insightedge-packager -Ppackage-open -Ddist.xap=$XAP_OPEN_URL ${package_args}"
        execute_command "Packaging $rep" "$1" "$cmd"

    elif [ "$rep" == "IE_PACKAGE_PREMIUM" ]; then
        cmd="mvn -e -B -Dmaven.repo.local=$M2/repository package -pl insightedge-packager -Ppackage-premium -Dxap.extension.jdbc=${XAP_JDBC_EXTENSION_URL} -Ddist.xap=$XAP_PREMIUM_URL ${package_args}"
        execute_command "Packaging $rep" "$1" "$cmd"
    else
        echo "Unknown type $rep in package_ie"
    fi
}



# Call maven deploy from directory $1
# It uses the target deploy:deploy to bypass the build.
# In case of none zero exit code exit code stop the release
function mvn_deploy {
    cmd="mvn -B -Dmaven.repo.local=$M2/repository -DskipTests deploy"
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

    if [ "$2" = "ie-open" ]; then
       sourceZipFileLocation="${sourceZipFileLocation}/open/"
       zipFileName="gigaspaces-insightedge-open-${FINAL_IE_BUILD_VERSION}.zip"
       targetPath="com/gigaspaces/insightedge/${IE_VERSION}/${FINAL_MAVEN_VERSION}"

    elif [ "$2" = "ie-premium" ]; then
       sourceZipFileLocation="${sourceZipFileLocation}/premium/"
       zipFileName="gigaspaces-insightedge-${FINAL_IE_BUILD_VERSION}.zip"
       targetPath="com/gigaspaces/insightedge/${IE_VERSION}/${FINAL_MAVEN_VERSION}"
    else
        echo "Unknown type $2 in upload_ie_zip"
    fi


    sourceZipFileLocation="${sourceZipFileLocation}/${zipFileName}"

    cmd="mvn -B -Dmaven.repo.local=$M2/repository com.gigaspaces:xap-build-plugin:deploy-native -Dput.source=${sourceZipFileLocation} -Dput.target=${targetPath}"

    echo "****************************************************************************************************"
    echo "uploading $2 zip"
    echo "Executing cmd: $cmd"
    echo "****************************************************************************************************"
    eval "$cmd"
    local r="$?"
    if [ "$r" -eq 1 ]
    then
        echo "[ERROR] failed to upload $2 zip, exit code:$r"
        exit 1
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



function getSHA {
    local curr=`pwd`
    cd "$1"
    echo $(git rev-parse HEAD)
    cd $curr
}

function release_ie {
    env

    local temp_branch_name="$BRANCH-$FINAL_IE_BUILD_VERSION"


    clean_old_tags "$ie_folder"
    clean_old_tags "$ie_open_folder"
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

#    announce_step "clean m2"
#    clean_m2


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
    rename_sbt "$ie_exm_folder"

    announce_step "rename ie version in poms [ie zeppelin]"
    rename_ie_version "$ie_zeppelin_folder"
    announce_step "rename version in poms [ie zeppelin]"
    rename_poms "$ie_zeppelin_folder"


    if [ ! -f "$WORKSPACE/spark-2.2.0-bin-hadoop2.7.tgz" ]
    then
        announce_step "Downloading spark distribution"
        wget https://d3kbcqa49mib13.cloudfront.net/spark-2.2.0-bin-hadoop2.7.tgz -P ${WORKSPACE}
        echo "Finished downloading spark"
    else
        echo "Found Spark distribution, download is skipped"
    fi

    export IE_SHA="$TAG_NAME"
    export LONG_TAG_NAME="$LONG_TAG_NAME"

    announce_step "executing maven install on ie"
    mvn_install_release "$ie_folder" "IE"
    echo "Done installing ie"

    announce_step "executing maven install on ie example"
    mvn_install_release "$ie_exm_folder" "IE_Example"
    echo "Done installing ie example"


    announce_step "executing maven install on ie zeppelin"
    mvn_install_release "$ie_zeppelin_folder" "IE_ZEPPELIN"
    echo "Done installing ie zeppelin"




    announce_step "package ie open package"
    package_ie "${ie_folder}" "IE_PACKAGE_OPEN"
    echo "Done package ie open"

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


    announce_step "DONE !"

}



function deploy_artifacts {
    announce_step "uploading ie-premium zip"
    upload_ie_zip "$ie_folder" "ie-premium"

    announce_step "uploading ie-premium zip"
    upload_ie_zip "$ie_folder" "ie-open"


	announce_step "deploying IE maven artifacts"
	mvn_deploy "$ie_folder"
}
function continuous {
    env

#    announce_step "clean m2"
#    clean_m2


#    announce_step "rename version in poms [ie]"
#    rename_poms "$ie_folder"
#    announce_step "rename xap version in poms [ie]"
#    rename_xap_version "$ie_folder"

#    announce_step "rename version in poms [ie example]"
#    rename_poms "$ie_exm_folder"
#    rename_sbt "$ie_exm_folder"

#    announce_step "rename ie version in poms [ie zeppelin]"
#    rename_ie_version "$ie_zeppelin_folder"
#    announce_step "rename version in poms [ie zeppelin]"
#    rename_poms "$ie_zeppelin_folder"


    if [ ! -f "$WORKSPACE/spark-2.2.0-bin-hadoop2.7.tgz" ]
    then
        announce_step "Downloading spark distribution"
        wget https://d3kbcqa49mib13.cloudfront.net/spark-2.2.0-bin-hadoop2.7.tgz -P ${WORKSPACE}
        echo "Finished downloading spark"
    else
        echo "Found Spark distribution, download is skipped"
    fi

    export IE_SHA=$(getSHA $ie_folder)

    announce_step "executing maven install on ie"
    mvn_install_cont "$ie_folder" "IE"
    echo "Done installing ie"

    announce_step "executing maven install on ie example"
    mvn_install_cont "$ie_exm_folder" "IE_Example"
    echo "Done installing ie example"


    announce_step "executing maven install on ie zeppelin"
    mvn_install_cont "$ie_zeppelin_folder" "IE_ZEPPELIN"
    echo "Done installing ie zeppelin"

   announce_step "package ie premium package"
    package_ie "$ie_folder" "IE_PACKAGE_PREMIUM"
    echo "Done package ie premium"


    announce_step "package ie open package"
    package_ie "$ie_open" "IE_PACKAGE_OPEN"
    echo "Done package ie open"

    announce_step "DONE !"
}
shift
$@