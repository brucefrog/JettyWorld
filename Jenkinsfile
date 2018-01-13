node {
    def server = Artifactory.server 'ART8080GCP'
	def rtMaven = Artifactory.newMavenBuild()
	def buildInfo
	
    stage('Checkout') {
    		// Get some code from a GitHub repository
		git 'https://github.com/brucefrog/JettyWorld'
    }
    stage('Build') {
		// Setup Artifactory resolution
		rtMaven.tool = 'Maven3.5.2'
		rtMaven.resolver server: server, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
        rtMaven.deployer server: server, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
		buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean package' 
		buildInfo.env.capture = true
		rtMaven.deployer.deployArtifacts buildInfo
		server.publishBuildInfo buildInfo
    }
    stage('Release') {
    		sh 'printenv'
    		 sh 'docker build -t docker.artifactory.bruce/onboard/hello .'
    		 sh 'docker push docker.artifactory.bruce/onboard/hello'
    }
}