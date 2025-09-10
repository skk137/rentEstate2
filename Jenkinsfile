pipeline {
	agent any

    environment {
		DOCKER_TOKEN = credentials('docker-push-secret')    // DEBUG
        DOCKER_USER = 'skk137'
        DOCKER_SERVER = 'ghcr.io'
        DOCKER_PREFIX = 'ghcr.io/skk137/rentestate2'
    }

    stages {

		stage('Checkout') {
			steps {
				git branch: 'api', url: 'https://github.com/skk137/rentEstate2.git'
            }
        }

        stage('run ansible pipeline') {
			steps {
				build job: 'ansible-job'
        }
    }

        stage('Test') {
			steps {
				sh '''
                    echo "Running Maven tests..."
                    chmod 777 mvnw
					./mvnw test
                '''
            }
        }

        stage('Docker build and push') {
			steps {
				script {
					// Δημιουργία TAG από commit + build ID
            def HEAD_COMMIT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
            def TAG = "${HEAD_COMMIT}-${env.BUILD_ID}"
            def IMAGE = "${DOCKER_PREFIX}:${TAG}"

            echo "Building and pushing Docker image: ${IMAGE}"

            // Τρέχει το Docker Spring playbook
            sh """
                export ANSIBLE_CONFIG=~/workspace/ansible-job/ansible.cfg
                ansible-playbook -i ~/workspace/ansible-job/hosts.yaml \
                    ~/workspace/ansible-job/playbooks/docker-spring.yaml \
                    -e DOCKER_PREFIX=$DOCKER_PREFIX \
                    -e TAG=$TAG \
                    -e DOCKER_USER=$DOCKER_USER \
                    -e DOCKER_TOKEN=$DOCKER_TOKEN \
                    -e DOCKER_SERVER=$DOCKER_SERVER
            """

            // Αποθήκευση image σε environment variable για το επόμενο stage
            env.NEW_IMAGE = IMAGE
        }
    }
}

stage('Deploy to Kubernetes') {
			steps {
				script {
					// Ορίζουμε διαδρομή για το virtualenv μέσα στο workspace του Jenkins
            def venv_dir = "${env.WORKSPACE}/venv-ansible"

            // Αν δεν υπάρχει, το δημιουργούμε με virtualenv (χρησιμοποιούμε --user για να μην χρειάζεται sudo)
            sh """
                python3 -m pip install --user virtualenv || true
                ~/.local/bin/virtualenv ${venv_dir} || true
                source ${venv_dir}/bin/activate
                pip install --upgrade pip
                pip install --upgrade ansible kubernetes openshift
            """

            // Εκτέλεση του Ansible playbook για Kubernetes deployment
            def HEAD_COMMIT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
            def TAG = "${HEAD_COMMIT}-${env.BUILD_ID}"
            sh """
                source ${venv_dir}/bin/activate
                export ANSIBLE_CONFIG=${env.WORKSPACE}/ansible-job/ansible.cfg
                ansible-playbook -i ${env.WORKSPACE}/ansible-job/hosts.yaml \
                    ${env.WORKSPACE}/ansible-job/playbooks/k8s-update-spring-deployment.yaml \
                    -e new_image=$DOCKER_PREFIX:$TAG
            """
        }
    }
}

}
}