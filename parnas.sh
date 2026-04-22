#!/usr/bin/env bash

APP_NAME=Parnas

CHANGELOG_URL='https://github.com/sndl/parnas/blob/master/CHANGELOG.md'
PARNAS_HOME=$HOME/.local/share/parnas
FIRST_ARG=$1

check_prerequisites () {
    local required_utils="java curl"

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
    local latest_ver=$(curl -s -o /dev/null -w "%{url_effective}" -L https://github.com/sndl/parnas/releases/latest | grep -oE '[^/]+$' | tr -d '[:space:]')
    local jar_name=${PARNAS_HOME}/${APP_NAME}.jar

    if [[ ${latest_ver} == "" || ${latest_ver} == "latest" ]]; then
        echo "WARNING: Couldn't connect to GitHub to check for updates."
        return
    fi

    local download_url="https://github.com/sndl/parnas/releases/download/${latest_ver}/parnas-${latest_ver#v}.jar"

    if [[ ${cur_ver} == "" || ${cur_ver} != ${latest_ver} || ! -f ${jar_name} || (${FIRST_ARG} == "check-updates" && ${cur_ver} != ${latest_ver}) ]]; then
        if [[ ${latest_ver} == $(cat ${PARNAS_HOME}/skip-update 2> /dev/null) && ${FIRST_ARG} != "check-updates" ]]
        then
            echo "You are using ${APP_NAME} ${cur_ver}, newer version ($(cat ${PARNAS_HOME}/skip-update)) is available but was skipped."
            return
        fi

        echo "New version of ${APP_NAME} is available - ${latest_ver}"
        echo "CHANGELOG: $CHANGELOG_URL"
        echo "Would you like to update? (yes/no):"

        read confirmation
        if [[ ${confirmation} == "yes" ]]; then
            echo "Updating..."
            curl ${download_url} -L -s -o ${jar_name} && echo ${latest_ver} > ${PARNAS_HOME}/version
            echo "Updated!"
        else
            echo "${latest_ver}" > ${PARNAS_HOME}/skip-update

            echo "Update to ${latest_ver} skipped."
            echo "You can always check for updates by running \"${APP_NAME} check-updates\""
        fi
    else
        echo "You are using latest version of ${APP_NAME} - ${cur_ver}"
    fi

    if [[ ${FIRST_ARG} == "check-updates" ]]
    then
        echo "Exiting..."
        exit 0
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
