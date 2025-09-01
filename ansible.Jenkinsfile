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

        stage('test connection to deploy env') {
        steps {
            sh '''
                ansible -i ~/workspace/ansible/hosts.yaml -m ping devops-vm-app,devops-vm-db
            '''
            }
        }

        stage('Install postgres') {
             when {
                expression { return params.INSTALL_POSTGRES }
            }
            steps {
                sh '''
                    export ANSIBLE_CONFIG=~/workspace/ansible/ansible.cfg
                    ansible-playbook -i ~/workspace/ansible/hosts.yaml -l devops-vm-db ~/workspace/ansible/playbooks/postgres.yaml
                '''
            }
        }

        stage('install springboot') {
             when {
                expression { return params.INSTALL_SPRING }
            }
            steps {
                sh '''
                    export ANSIBLE_CONFIG=~/workspace/ansible/ansible.cfg
                    ansible-playbook -i ~/workspace/ansible/hosts.yaml -l devops-vm-app ~/workspace/ansible/playbooks/spring.yaml
                '''
            }
        }
}
}