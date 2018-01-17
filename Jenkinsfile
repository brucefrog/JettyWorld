node {
    def server = Artifactory.server 'ART8080GCP'
	def rtMaven = Artifactory.newMavenBuild()
	def artDocker = Artifactory.docker server: server, host: "tcp://localhost:1234"
	def imageName = 'docker.artifactory.bruce/onboard/hello:' + env.BUILD_NUMBER
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
		buildInfo.retention maxBuilds: 10
		rtMaven.deployer.deployArtifacts buildInfo
		// server.publishBuildInfo buildInfo
    }
    stage('Verify Jar') {
    		rtMaven.tool = 'Maven3.5.2'
    		parallel apprun: {
    			timeout(time: 10, unit: 'SECONDS') {
		    		def buildInfo2 = rtMaven.run pom: 'pom.xml', goals: 'exec:exec'
		    		// buildInfo.append buildInfo2
	    		}
    		},
    		apptest: {
    			sleep 5
    			sh 'curl "http://localhost:6800/hello"'
    			sh 'curl "http://localhost:6800/shutdown"'
    		}
    }
    
	
    stage('Docker Image') {
			def dockerImage = docker.build(imageName)
			// dockerImage.push()
			def dockInfo = artDocker.push imageName, 'docker', buildInfo 
			// buildInfo.append dockerInfo
			server.publishBuildInfo(buildInfo)
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
    stage('Verify Image') {
        sh 'docker rmi ' + imageName
    		// docker.pull(imageName)
    		docker.image(imageName).withRun('-p 6800:6800') {c ->
                sleep 5
                sh 'curl "http://localhost:6800/"'
            }
    }
    stage('Promote') {
		def promotionConfig = [
		    // Mandatory parameters
		    'buildName'          : buildInfo.name,
		    'buildNumber'        : buildInfo.number,
		    'targetRepo'         : 'libs-release-local',
		 
		    // Optional parameters
		    'comment'            : 'Promoting successfully tested jar and docker image',
		    'sourceRepo'         : 'libs-snapshot-local',
		    'status'             : 'Released',
		    'includeDependencies': true,
		    'copy'               : true,
		    'properties'         : {
		    	     "Jar validated" : "true"
		    }
		    // 'failFast' is true by default.
		    // Set it to false, if you don't want the promotion to abort upon receiving the first error.
		    'failFast'           : false
		]
		 
		// Promote build
		server.promote promotionConfig    
    }
}