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
        sh 'mvn -B --version'
      }
    }
    stage('Test') {
      steps {
        sh 'mvn -B clean test'
      }
    }
    stage('Deploy') {
      when {
        anyOf {
          branch '1.3.develop'
          branch '1.3.master'
        }
      }
      steps {
        sh 'mvn -B -P deploy clean deploy'
      }
    }
    stage('Snapshot Site') {
      when {
        branch '1.3.develop'
      }
      steps {
        sh 'mvn -B site-deploy'
      }
    }
    stage('Release Site') {
      when {
        branch '1.3.master'
      }
      steps {
        sh 'mvn -B -P gh-pages-site site site:stage scm-publish:publish-scm'
      }
    }
  }
}