#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2023-07'

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
static final String[] OS_VERSIONS = ['linux', 'macos', 'windows']

@groovy.transform.Field
static final String[] PYTHON_LINUX_ENV = [
    //'py27_knime',
    //'py36_knime_dl_cpu',
    //'py36_knime_dl_gpu',
    //'py36_knime_tf2_cpu', 
    //'py36_knime_tf2_gpu',
    //'py38_knime',
    //'py38_knime_tf2_cpu', 
    'py39_knime',
    'py39_knime_tf2_cpu'
]

@groovy.transform.Field
static final String[] PYTHON_MACOS_64_ENV = [
    //'py27_knime',
    //'py36_knime_dl_cpu',
    //'py36_knime_tf2_cpu',
    //'py36_knime',
    //'py37_knime_dl_cpu',
    //'py37_knime',
    //'py38_knime_tf2_cpu',
    //'py38_knime',
    'py39_knime_tf2_cpu',
    'py39_knime'
]

@groovy.transform.Field
static final String[] PYTHON_WIN_64_ENV = [
    'py27_knime',
    //'py36_knime',
    //'py36_knime_dl_cpu',
    //'py36_knime_dl_gpu',
    'py36_knime_tf2_cpu',
    'py36_knime_tf2_gpu',
    'py37_knime',
    'py37_knime_dl_cpu',
    'py38_knime',
    'py38_knime_tf2_cpu',
    'py39_knime_tf2_cpu',
    'py39_knime',
]

try {


    def OSCONDABUILD = [:]

    /*


    // linux-64
        
    OSCONDABUILD["linux-64"] = {
        node('ubuntu22.04 && workflow-tests') {
            stage('Prepare Linux') {
                env.lastStage = env.STAGE_NAME
                checkout scm
                sh(
                    label: 'Check env list',
                    script: "micromamba env list",
                )        
                sh(
                    label: 'Check micromamba version',
                    script: "micromamba info",
                )                   
                sh(
                    label: 'Check micromamba version',
                    script: "conda info",
                )        
            }

            String ymlPath = "${env.WORKSPACE}/org.knime.python2.envconfigs/envconfigs/linux"

            for (pyEnv in PYTHON_LINUX_ENV) {
                stage("Linux ${pyEnv}") {
                    sh(
                        label: 'create micromamba env',
                        script: "micromamba env create \
                            -f ${ymlPath}/${pyEnv}.yml \
                            --yes \
                            --json",
                    )
                    sh(
                        label: 'create conda env',
                        script: "conda env create \
                            -f ${ymlPath}/${pyEnv}.yml \
                            -d \
                            -q \
                            --force --json",
                    )
                }
            }
            sh(
                label: 'create list',
                script: "micromamba env list",
            )
        }
    }
    */

    // osx-64
    /*
    OSCONDABUILD["osx-64"] = {
        node('macosx && workflow-tests && python3' ) {

            stage('Prepare MacOS') {
                env.lastStage = env.STAGE_NAME
                checkout scm
                sh(
                    label: 'Check env list',
                    script: "micromamba env list",
                )                 
                sh(
                    label: 'Check env list',
                    script: "conda info",
                )    
            }

            String rootPrefix = "${env.WORKSPACE}/org.knime.python2.envconfigs/envs"
            String ymlPath = "${env.WORKSPACE}/org.knime.python2.envconfigs/envconfigs/macos"
            
            for (pyEnv in PYTHON_MACOS_64_ENV) {
                stage("MacOSX ${pyEnv}") {
                    sh "echo ${env.WORKSPACE}"
                    sh(
                        label: 'micromamba create env', 
                        script: "micromamba env create  \
                        -p ${rootPrefix}/${pyEnv} \
                        -f ${ymlPath}/${pyEnv}.yml \
                        --json --quiet --yes",
                    )                    
                    sh(
                        label: 'conda create env', 
                        script: "conda env create  \
                        -p ${rootPrefix}/${pyEnv} \
                        -f ${ymlPath}/${pyEnv}.yml \
                        -q \
                        -d \
                        --json --force",
                    )
                }
            }
            sh(
                label: 'Check env list',
                script: "micromamba env list",
            )        
        }
    }
    */

    // windows
    OSCONDABUILD["win-64"] = {
        node('windows && workflow-tests & python3') {

            // String mambaRoot = "C:\\\\Users\\\\jenkins\\\\micromamba"
            String condaRoot = "C:\\\\Users\\\\jenkins\\\\Miniconda3\\\\"
            String envPrefix = "org.knime.python2.envconfigs\\\\envconfigs\\\\windows"
            String envLocation = env.WORKSPACE.replaceAll('/', '%2F')
            String condaBat = "C:/Users/jenkins/Miniconda3/condabin/conda.bat"
            // String condaRoot = "C:/Users/jenkins/Miniconda3/"

            sh(
                label: 'list WORKSPACE ',
                script: "${env.WORKSPACE} env list"
            )
            sh(
                label: 'list WORKSPACE ',
                script: "${env.WORKSPACE.replaceAll('/', '%2F')} env list"
            )
            /*
            String mambaRoot = "C:/Users/jenkins/micromamba/"
            String condaRoot = "C:/Users/jenkins/Miniconda3/"
            String condaBat = "C:/Users/jenkins/Miniconda3/condabin/conda.bat"
            String envPrefix = "org.knime.python2.envconfigs/envconfigs/windows"
            */

            environment { // necessary for Scripts\wheel.exe
                MAMBA_ROOT_PREFIX = "${condaRoot}"
                MAMBA_ROOT="${mambaRoot}"
                CONDA_ROOT="${condaRoot}"
                CONDA_BAT="${condaBat}"
                ENV_PREFIX="${envPrefix}"
            }
            /*
            stage('Prepare Windows') {
                env.lastStage = env.STAGE_NAME
                checkout scm
            environment { // necessary for Scripts\wheel.exe
                MAMBA_ROOT_PREFIX = 'C:\\\\Users\\\\jenkins\\\\Miniconda3\\\\'
            }


            String mambaPrefix = "org.knime.python2.envconfigs\\\\envconfigs\\\\windows"
            String rootPrefix = "C:\\\\Users\\\\jenkins\\\\Miniconda3\\\\"
                        label: 'micromamba build',
                        script: "micromamba.exe env create  \
                            -p ${rootPrefix}\\${pyEnv} \
                            -f ${mambaPrefix}\\\\${pyEnv}.yml \
                            -r ${rootPrefix} \
                            --json --yes"



            for (pyEnv in PYTHON_WIN_64_ENV) {
                stage("Windows ${pyEnv}") {
                    sh(
                        label: 'conda build',
                        script: "C:\\\\Users\\\\jenkins\\\\Miniconda3\\\\Scripts\\\\conda.exe env create \
                            -p ${rootPrefix}\\${pyEnv} \
                            -f ${mambaPrefix}\\\\${pyEnv}.yml \
                            -r ${rootPrefix} \
                            --json --yes"
                    )
                    sh(
                        label: 'micromamba build',
                        script: "micromamba.exe env create  \
                            -p ${rootPrefix}\\${pyEnv} \
                            -f ${mambaPrefix}\\\\${pyEnv}.yml \
                            -r ${rootPrefix} \
                            --json --yes"
                    )
                }
            }
                sh(
                    label: 'micromamba ls',
                    script: "ls ${mambaRoot}"
                )
                
                sh(
                    label: 'micromamba info',
                    script: "micromamba.exe info"
                )
                sh( 
                    label: 'conda info',
                    script:  "${condaBat} info"
                )
            }
            */

            for (pyEnv in PYTHON_WIN_64_ENV) {
                /*
                stage("micromamba ${pyEnv} ") {
                    script {
                        // Execute the bash script
                        def exitCode = sh(returnStatus: true, script: "micromamba.exe env create  \
                            -f ${envPrefix}\\\\${pyEnv}.yml \
                            -p ${condaRoot} \
                            --json --yes"
                        )
                        
                        if (exitCode != 0) {
                            println "Bash script failed with exit code: ${exitCode}"
                            unstable("Unstable Mamba")
                        }
                    }
                }
                */
                                
                stage("conda ${pyEnv} ") {
                    script {
                        // Execute the bash script
                        def exitCode = sh(returnStatus: true, script: "${condaBat} env create -p ${envLocation}\\\\${pyEnv} \
                            -f ${envPrefix}\\${pyEnv}.yml -r ${condaRoot} -q -d --json --force")
                        
                        if (exitCode != 0) {
                            println "Bash script failed with exit code: ${exitCode}"
                            unstable("Unstable Conda")

                        }
                    }
                }
            }
            stage(("List Envs"))
            sh(
                label: 'list environment ',
                script: "micromamba.exe env list"
            )
            sh(
                label: 'list environment ',
                script: "${condaBat} env list"
            )
        }
    }
    parallel(OSCONDABUILD)
 } catch (ex) {
     currentBuild.result = 'FAILURE'
     throw ex
 } finally {
     notifications.notifyBuild(currentBuild.result);
 }


try {
    knimetools.defaultTychoBuild('org.knime.update.python.legacy', 'maven && python2 && python3 && java17')

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
                    'knime-core-arrow',
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
                        'knime-core-arrow',
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
