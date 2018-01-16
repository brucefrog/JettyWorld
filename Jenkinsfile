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
		// server.publishBuildInfo buildInfo
    }
    stage('Verify Jar') {
    		rtMaven.tool = 'Maven3.5.2'
    		parallel apprun: {
    			timeout(time: 10, unit: 'SECONDS') {
		    		def buildInfo2 = rtMaven.run pom: 'pom.xml', goals: 'exec:exec'
		    		buildInfo.append buildInfo2
	    		}
    		},
    		apptest: {
    			sleep 5
    			sh 'curl "http://localhost:6800/hello"'
    			sh 'curl "http://localhost:6800/shutdown"'
    		}
    }
    
	def imageName = 'docker.artifactory.bruce/onboard/hello:' + env.BUILD_NUMBER
	def artDocker= Artifactory.docker()
	
    stage('Publish Docker Image') {
			def dockerImage = docker.build(imageName)
			server.publishBuildInfo(buildInfo)
			dockerImage.push()
    }
    stage('Xray Scan') {
          def xrayConfig = [
            //Mandatory parameters
            'buildName'         : env.JOB_NAME,
            'buildNumber'       : env.BUILD_NUMBER,

            //Optional
            'failBuild'        : false
          ]

          // Scan xray build
          def xrayResults = server.xrayScan xrayConfig
          // Print full report from xray
          echo xrayResults as String
    }
    stage('Verify Docker Image') {
    		// server.pull(imageName)
    		docker.image(imageName).withRun('-p 6800:6800') {c ->
                sleep 5
                sh 'curl "http://localhost:6800/"'
            }
    }
}