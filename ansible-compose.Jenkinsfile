

pipeline {

    agent any

    stages {

        stage('run ansible pipeline') {
            steps {
                build job: 'ansible-job'
            }
        }

        stage('test connection to deploy env') {
                steps {
                    sh '''
                        ansible -i ~/workspace/ansible-job/hosts.yaml -m ping devops-vm-app,devops-vm-db
                    '''
                    }
                }

        stage('Install postgres') {
            steps {
                sh '''
                    export ANSIBLE_CONFIG=~/workspace/ansible-job/ansible.cfg
                    ansible-playbook -i ~/workspace/ansible-job/hosts.yaml -l devops-vm-app ~/workspace/ansible-job/playbooks/docker-spring.yaml
                '''
            }
        }

}
}