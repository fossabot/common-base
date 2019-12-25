pipeline {
  agent {
    label 'maven'
  }
  tools {
    jdk 'jdk8'
    maven 'm3'
  }
  stages {
    stage('Tools') {
      steps {
        sh 'java -version'
        sh 'mvn --version'
      }
    }
    stage('Test') {
      steps {
        sh 'mvn clean test'
      }
    }
  }
}