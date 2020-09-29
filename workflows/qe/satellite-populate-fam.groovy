@Library("github.com/SatelliteQE/robottelo-ci") _

pipeline {
    agent { label 'sat6-rhel7' }
    environment {
        OS_VERSION = '7'
        DISTRO = 'rhel7'
        ANSIBLE_HOST_KEY_CHECKING = 'False'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr:'32'))
    }
    stages {
        stage('Setup Environment') {
            dir('ansible') {

                virtEnv('ansible-venv', 'pip install -r ansible-requirements.txt')
                virtEnv('ansible-venv', 'ansible-galaxy collection install -r requirements.yml')
    }
        }
        stage('Source Config and Variables') {
            script {
                configFileProvider(
                    [configFile(fileId: 'bc5f0cbc-616f-46de-bdfe-2e024e84fcbf', variable: 'CONFIG_FILES')]) {
                        sh_venv '''
                            source ${CONFIG_FILES}
                        '''
                        load('config/sat6_repos_urls.groovy')
                        load('config/subscription_config.groovy')
                        sh_venv '''
                            sed -i "s/foreman.example.com/${SERVER_HOSTNAME}/g" ansible/inventory
                        '''
                    }
                }
            }
        stage('Run Populate playbook') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: 'id_hudson_rsa', keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: 'userName')]) {
                        virtEnv('ansible-venv', 'ansible-playbook -i ansible/inventory ansible/playbooks/satellite_68_populate.yml')
                            }
                    }
                }
            }
        }
    }