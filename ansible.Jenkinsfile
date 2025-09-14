pipeline {

    agent any

    parameters {
        booleanParam(name: 'INSTALL_POSTGRES', defaultValue: true, description: 'Install PostgreSQL')
        booleanParam(name: 'INSTALL_SPRING', defaultValue: true, description: 'Install Spring Boot app')
    }

    stages {

        stage('run ansible pipeline') {
            steps {
                build job: 'ansible-job'
            }
        }
         // Test connection to target servers
        stage('test connection to deploy env') {
        steps {
            sh '''
                ansible -i ~/workspace/ansible-job/hosts.yaml -m ping devops-vm-app,devops-vm-db
            '''
            }
        }
        // Install PostgreSQL on DB server
        stage('Install postgres') {
             when {
                expression { return params.INSTALL_POSTGRES }
            }
            steps {
                sh '''
                    export ANSIBLE_CONFIG=~/workspace/ansible-job/ansible.cfg
                    ansible-playbook -i ~/workspace/ansible-job/hosts.yaml -l devops-vm-db ~/workspace/ansible-job/playbooks/postgres.yaml
                '''
            }
        }

         // Install Spring Boot app on app server
        stage('install springboot') {
             when {
                expression { return params.INSTALL_SPRING }
            }
            steps {
                sh '''
                    export ANSIBLE_CONFIG=~/workspace/ansible-job/ansible.cfg
                    ansible-playbook -i ~/workspace/ansible-job/hosts.yaml -l devops-vm-app ~/workspace/ansible-job/playbooks/spring.yaml
                '''
            }
        }
}
}