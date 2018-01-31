node {
    def server = Artifactory.server 'ART8080GCP'
	def rtMaven = Artifactory.newMavenBuild()
	def artDocker = Artifactory.docker server: server, host: "tcp://localhost:2375"
	def image = 'docker.artifactory.bruce/onboard/hello'
	def buildImage = image + ":" + env.BUILD_NUMBER
	def buildInfo
	
	rtMaven.tool = 'Maven3.5.2'
	rtMaven.resolver server: server, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
    rtMaven.deployer server: server, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
	
    stage('Checkout') {
    		// Get some code from a GitHub repository
		git url: 'https://github.com/brucefrog/JettyWorld', branch: env.BRANCH_NAME
    }
    stage('Java Build') {
		// Setup Artifactory resolution
        rtMaven.deployer.addProperty("MyProp","Hello")
        if (params.RELEASE_PROMOTION == 'TRUE') {
        		rtMaven.deployer deployArtifacts: 'false'
			buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean release:clean release:prepare-with-pom -Dresume=false'
        } else {
    			buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean package' 
        }
		buildInfo.env.capture = true
		buildInfo.retention maxBuilds: 10
    }
    stage('Unit Test') {
    		parallel apprun: {
    			timeout(time: 30, unit: 'SECONDS') {
	    			dir("java") {
			        rtMaven.deployer.addProperty("MyProp2","Hello...")
			        echo "% rtMaven run exec:exec"
			    		def buildInfo2 = rtMaven.run pom: 'pom.xml', goals: 'exec:exec'
			    		// buildInfo.append buildInfo2 
			    	}
	    		}
    		},
    		apptest: {
    			sleep 10
    			sh 'curl "http://localhost:6800/hello"'
    			sh 'curl "http://localhost:6800/shutdown"'
    		}
    }
	stage('Deploy') {
		if (params.RELEASE_PROMOTION == 'TRUE') {
	        rtMaven.deployer.addProperty("Release","promoted")
	        rtMaven.deployer deployArtifacts: 'false'
			// def buildInfo6 = rtMaven.run pom: 'pom.xml', goals: 'install'
			def buildInfo6 = rtMaven.run pom: 'pom.xml', goals: 'release:perform'
	        echo "% rtMaven.deployer.deployArtifacts buildInfo6"
			// rtMaven.deployer.deployArtifacts buildInfo6
			// buildInfo.append buildInfo6
	        echo "% server.publishBuildInfo buildInfo6"
			server.publishBuildInfo buildInfo6
			sleep 30 
		} else {
			def buildInfo5 = rtMaven.run pom: 'pom.xml', goals: 'install'
			rtMaven.deployer.deployArtifacts buildInfo5 
			// buildInfo.append buildInfo5
			server.publishBuildInfo buildInfo5
		}
	}
    stage('Xray Scan') {
		rtMaven.tool = 'Maven3.5.2'
		// rtMaven.deployer.artifactDeploymentPatterns.addExclude("*.pom")
		// def buildInfo3 = rtMaven.run pom: 'pom.xml', goals: 'install' 
		// buildInfo.append buildInfo3
        // rtMaven.deployer.addProperty("JarVerify","Passed")
		// rtMaven.deployer.deployArtifacts buildInfo
		// server.publishBuildInfo buildInfo
		
          def xrayConfig = [
            //Mandatory parameters
            'buildName'         : env.JOB_NAME,
            'buildNumber'       : env.BUILD_NUMBER,

            //Optional
            'failBuild'        : false
          ]

          // Scan xray build
          // def xrayResults = server.xrayScan xrayConfig
          // Print full report from xray
          // echo xrayResults as String
    }
}
