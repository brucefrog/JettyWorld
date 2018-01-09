node {
    def server = Artifactory.server 'ART8080GCP'
	def rtMaven = Artifactory.newMavenBuild()
	def buildInfo
	
    stage('Checkout') {
    		// Get some code from a GitHub repository
		git 'https://github.com/brucefrog/JettyWorld'
		// Setup Artifactory resolution
		rtMaven.resolver server: server, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
		// Setup Maven tool
		rtMaven.tool = 'Maven3.5.2'
    }
    stage('Build') {
		sh 'printenv'
		// sh "mvn -Dmaven.test.failure.ignore clean package"
		buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean package' 
		buildInfo.env.capture = true
    }
    stage('Publish') {
        rtMaven.deployer server: server, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
		rtMaven.deployer.artifactDeploymentPatterns.addInclude("*").addExclude("*.jar")
		rtMaven.tool = 'Maven3.5.2'
		buildInfo = rtMaven.run pom: 'pom.xml', goals: 'install' 
		rtMaven.deployer.deployArtifacts buildInfo
    }
}