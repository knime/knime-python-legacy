#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2023-12'

@groovy.transform.Field
static final String[] PYTHON_VERSIONS = ['36', '37', '38']

@groovy.transform.Field
static final String DEFAULT_PYTHON_VERSION = '36'

library "knime-pipeline@$BN"

def baseBranch = (BN == KNIMEConstants.NEXT_RELEASE_BRANCH ? "master" : BN.replace("releases/",""))

properties([
    pipelineTriggers([
        upstream('knime-filehandling/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    parameters(workflowTests.getConfigurationsAsParameters() + getPythonParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])


@groovy.transform.Field
static final String[] PYTHON_MAC_64_ENV = [
    'py27_knime',
    'py36_knime_dl_cpu',
    'py36_knime_tf2_cpu',
    'py36_knime',
    'py37_knime_dl_cpu',
    'py37_knime',
    'py38_knime_tf2_cpu',
    'py38_knime',
    'py39_knime_tf2_cpu',
    'py39_knime'
]

try {
    knimetools.defaultTychoBuild('org.knime.update.python.legacy', 'maven && python2 && python3 && java17')

    // MacOS
    def MACOSCONDABUILD = [:]
    for (envFile in PYTHON_MAC_64_ENV) {
        def envString = new String(envFile)
        MACOSCONDABUILD["${envString}"] = {
            buildCondaEnvironmentMac(envString)
        }
    }
    // Parallel
    parallel(MACOSCONDABUILD)

    def parallelConfigs = [:]
    for (py in PYTHON_VERSIONS) {
        if (params[py]) {
            // need to create a deep copy here, otherwise Jenkins will use
            // the last selected option for everything
            def python_version = new String(py)
            parallelConfigs["${python_version}"] = {
                runPython3MultiversionWorkflowTestConfig(python_version, baseBranch)
            }
        }
    }

    // legacy tests
    parallelConfigs["Python 2.7"] = {
        runPython27WorkflowTests(baseBranch)
    }

    parallel(parallelConfigs)

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
 } catch (ex) {
     currentBuild.result = 'FAILURE'
     throw ex
 } finally {
     notifications.notifyBuild(currentBuild.result);
 }

def buildCondaEnvironmentMac(String envFile) {
    node('macosx && workflow-tests && python3') {
        String ymlPath = "org.knime.python2.envconfigs/envconfigs/macos"

        stage("$envFile") {
            env.lastStage = env.STAGE_NAME
            checkout scm

            sh(
                label: "Info",
                script: """#!/bin/sh
                    conda info
                """
            )
            sh(
                label: "Install $envFile",
                script: """#!/bin/sh
                    conda env create -f $ymlPath/${envFile}.yml -p $WORKSPACE/$envFile --quiet --json --solver=classic
                """
            )
            sh(
                label: "List $envFile",
                script: """#!/bin/sh
                    conda list -p $WORKSPACE/$envFile
                """
            )
        }
    }
}

/**
* Return parameters to select python version to run workflowtests with
*/
def getPythonParameters() {
    def pythonParams = []
    for (c in PYTHON_VERSIONS) {
        pythonParams += booleanParam(defaultValue: c == DEFAULT_PYTHON_VERSION, description: "Run workflowtests with Python ${c}", name: c)
    }
    return pythonParams
}

def runPython27WorkflowTests(String baseBranch){
    withEnv([ "KNIME_POSTGRES_USER=knime01", "KNIME_POSTGRES_PASSWORD=password",
              "KNIME_MYSQL_USER=root", "KNIME_MYSQL_PASSWORD=password",
              "KNIME_MSSQLSERVER_USER=sa", "KNIME_MSSQLSERVER_PASSWORD=@SaPassword123",]){

        workflowTests.runTests(
            testflowsDir: "Testflows (${baseBranch})/knime-python-legacy/python2.7",
            dependencies: [
                repositories: [
                    'knime-chemistry',
                    'knime-python',
                    'knime-database',
                    'knime-office365',
                    'knime-datageneration',
                    'knime-distance',
                    'knime-ensembles',
                    'knime-filehandling',
                    'knime-jep',
                    'knime-jfreechart',
                    'knime-js-base',
                    'knime-kerberos',
                    'knime-python-legacy',
                    'knime-core-columnar',
                    'knime-testing-internal',
                    'knime-xml',
                    'knime-conda'
                ],
                ius: [ 'org.knime.features.ext.jython.feature.group', 'org.knime.features.chem.types.feature.group' ]
            ],
            extraNodeLabel: 'python2',
            sidecarContainers: [
                    [ image: "${dockerTools.ECR}/knime/postgres:12", namePrefix: "POSTGRES", port: 5432,
                        envArgs: [
                            "POSTGRES_USER=${env.KNIME_POSTGRES_USER}", "POSTGRES_PASSWORD=${env.KNIME_POSTGRES_PASSWORD}",
                            "POSTGRES_DB=knime_testing"
                        ]
                    ],
                    [ image: "${dockerTools.ECR}/knime/mysql5", namePrefix: "MYSQL", port: 3306,
                        envArgs: ["MYSQL_ROOT_PASSWORD=${env.KNIME_MYSQL_PASSWORD}"]
                    ],
                    [ image: "${dockerTools.ECR}/knime/mssql-server", namePrefix: "MSSQLSERVER", port: 1433,
                        envArgs: ["ACCEPT_EULA=Y", "SA_PASSWORD=${env.KNIME_MSSQLSERVER_PASSWORD}", "MSSQL_DB=knime_testing"]
                    ]
            ],
        )
    }
}

def runPython3MultiversionWorkflowTestConfig(String pythonVersion, String baseBranch) {
    withEnv([ "KNIME_WORKFLOWTEST_PYTHON_VERSION=${pythonVersion}" ]) {
        stage("Workflowtests with Python: ${pythonVersion}") {
            workflowTests.runTests(
                testflowsDir: "Testflows (${baseBranch})/knime-python-legacy/python3.multiversion",
                dependencies: [
                    repositories: [
                        'knime-chemistry',
                        'knime-python',
                        'knime-database',
                        'knime-office365',
                        'knime-datageneration',
                        'knime-distance',
                        'knime-ensembles',
                        'knime-filehandling',
                        'knime-jep',
                        'knime-jfreechart',
                        'knime-js-base',
                        'knime-kerberos',
                        'knime-python-legacy',
                        'knime-core-columnar',
                        'knime-testing-internal',
                        'knime-xml',
                        'knime-conda'
                    ],
                    ius: [ 'org.knime.features.chem.types.feature.group' ]
                ],
                extraNodeLabel: 'python-all'
            )
        }
    }
}

/* vim: set shiftwidth=4 expandtab smarttab: */
