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
    		if (env.BRANCH_NAME) {
			git url: 'https://github.com/brucefrog/JettyWorld', branch: env.BRANCH_NAME
		} else {
			git url: 'https://github.com/brucefrog/JettyWorld'
		}
    }
    stage('Java Build') {
		// Setup Artifactory resolution
        rtMaven.deployer.addProperty("MyProp","Hello")
		buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean package'
		buildInfo.env.capture = true
		buildInfo.retention maxBuilds: 10
    }
    stage('Unit Test') {
    		parallel apprun: {
    			timeout(time: 30, unit: 'SECONDS') {
	    			dir("java") {
			    		def buildInfo2 = rtMaven.run pom: 'pom.xml', goals: 'exec:exec'
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
			def buildInfo5 = rtMaven.run pom: 'pom.xml', goals: 'install'
			buildInfo.append buildInfo5
			
			// rtMaven.deployer.deployArtifacts buildInfo5 
			// buildInfo.append buildInfo5
			// server.publishBuildInfo buildInfo5
	}
    stage('Dockerize') {
    		dir("docker") {
			def dockerImage = docker.build(buildImage)
			dockerImage.tag("latest")
			def dockInfo = artDocker.push buildImage, 'docker', buildInfo 
			// dockerImage.push("latest")
			artDocker.push image+":latest", 'docker'
			// buildInfo.append dockerInfo
		}
    }
    stage('Verify') {
        sh 'docker rmi ' + image
        sh 'docker rmi ' + buildImage

    		docker.image(buildImage).withRun('-p 6800:6800') {c ->
                sleep 5
                sh 'curl "http://localhost:6800/"'
        }
        sh 'docker rmi ' + buildImage
    }
    stage('Xray Scan') {
		  server.publishBuildInfo buildInfo
		  sleep 20
		  
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
          // echo xrayResults as String
    }

}
