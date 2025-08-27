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
				sh '''
                    HEAD_COMMIT=$(git rev-parse --short HEAD)
                    TAG=$HEAD_COMMIT-$BUILD_ID
                    docker build --rm -t $DOCKER_PREFIX:$TAG -t $DOCKER_PREFIX:latest -f Dockerfile .
                    echo $DOCKER_TOKEN | docker login $DOCKER_SERVER -u $DOCKER_USER --password-stdin
                    docker push $DOCKER_PREFIX --all-tags
                '''
            }
        }

        stage('Deploy to Kubernetes') {
			steps {
				echo "XREIAZETAI NA VALOYME  Ansible repo and config. " //
            }
        }

        // AN THELETE NA SETTAROUME SMTP SERVER GIA MAIL NOTIFICATIONS

    }

}