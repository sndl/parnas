#!/usr/bin/env bash

APP_NAME=Parnas

LATEST_BUILD_INFO=$(curl -s https://api.github.com/repos/sndl/parnas/releases/latest)
CHANGELOG_URL='https://github.com/sndl/parnas/blob/master/CHANGELOG.md'
PARNAS_HOME=$HOME/.local/share/parnas

check_prerequisites () {
    local required_utils="java curl jq"

    for util in ${required_utils}
    do
        which ${util} > /dev/null
        if [[ $? -eq 1 ]]; then
            echo "\"$util\" has to be installed to use the wrapper"
            exit 1
        fi
    done
}

check_config () {
    [[ ! -d ${PARNAS_HOME} ]] && mkdir -p ${PARNAS_HOME}
    [[ ! -f ${PARNAS_HOME}/version ]] && touch ${PARNAS_HOME}/version
}

check_for_updates () {
    local cur_ver=$(cat ${PARNAS_HOME}/version)
    local latest_ver=$(echo ${LATEST_BUILD_INFO} | jq -r '.name')
    local jar_name=${PARNAS_HOME}/${APP_NAME}.jar

    if [[ ${latest_ver} == "" ]]; then
        echo "WARNING: Couldn't connect to GitHub to check for updates."
        return
    fi

    if [[ ${cur_ver} == "" || ${cur_ver} != ${latest_ver} || ! -f ${jar_name} ]]; then
        echo "New version of ${APP_NAME} is available - ${latest_ver}"
        echo "CHANGELOG: $CHANGELOG_URL"
        echo "Would you like to update? (yes/no):"

        read confirmation
        if [[ ${confirmation} == "yes" ]]; then
            echo "Updating..."
            curl $(echo ${LATEST_BUILD_INFO} | jq -r '.assets[].browser_download_url') -L -s -o ${jar_name} && echo ${latest_ver} > ${PARNAS_HOME}/version
            echo "Updated!"
        else
            echo "Update skipped."
        fi

    fi
}

execute () {
    java -jar ${PARNAS_HOME}/${APP_NAME}.jar "$@"
}

main () {
    check_prerequisites
    check_config
    check_for_updates
    execute "$@"
}

main "$@"

exit 0
