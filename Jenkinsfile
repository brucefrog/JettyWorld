node {
    def server = Artifactory.server 'ART8080GCP'
	def rtMaven = Artifactory.newMavenBuild()
	def artDocker = Artifactory.docker server: server, host: "tcp://localhost:2375"
	def image = 'docker.artifactory.bruce/onboard/hello'
	def buildImage = image + ":" + env.BUILD_NUMBER
	def baseVersion = "3.1"
	def buildInfo = Artifactory.newBuildInfo()
	
	buildInfo.env.capture = true
	buildInfo.retention maxBuilds: 10
	
	if (env.BRANCH_NAME) {
		// override build name to avoid xray not recognizing :: in build name
		buildInfo.name = 'JettyWorld-' + env.BRANCH_NAME
	}
		
	
    stage('Checkout') {
    		// Get some code from a GitHub repository
    		if (env.BRANCH_NAME) {
			git url: 'https://github.com/brucefrog/JettyWorld', branch: env.BRANCH_NAME
		} else if (param.BRANCH_NAME) {
			git url: 'https://github.com/brucefrog/JettyWorld', branch: param.BRANCH_NAME
		} else {
			git url: 'https://github.com/brucefrog/JettyWorld'
		}
    }
    stage('Configure') {
		rtMaven.tool = 'Maven3.5.2'
		rtMaven.resolver server: server, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
	    rtMaven.deployer server: server, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
    		
    		// Transforming pom version number
    		def buildVersion
    		if (env.BRANCH_NAME == 'master') {
    			buildVersion = baseVersion + "." + env.BUILD_NUMBER
    		} else if (env.BRANCH_NAME == 'snapshot') {
    			buildVersion = baseVersion + "." + env.BUILD_NUMBER + "-SNAPSHOT"
    		} else {
    			buidlVersion = baseVersion
    		}
		def descriptor = Artifactory.mavenDescriptor()
		descriptor.version = '1.x.y'
		descriptor.pomFile = 'pom.xml'
		descriptor.setVersion "bruce.jfrog:JettyParent", buildVersion
		descriptor.transform()
		descriptor.setVersion "bruce.jfrog:JettyWorld", buildVersion
		descriptor.transform()
    		
    }
    stage('Java Build') {
		rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
    }
    stage('Unit Test') {
    		parallel apprun: {
    			timeout(time: 60, unit: 'SECONDS') {
	    			dir("java") {
	    				try {
				    		def buildInfo2 = rtMaven.run pom: 'pom.xml', goals: 'exec:exec'
	    				} catch (error) {
	    					retry(2) {
	    						sleep 10
					    		def buildInfo2 = rtMaven.run pom: 'pom.xml', goals: 'exec:exec'
	    					}
	    				}
			    	}
	    		}
    		},
    		apptest: {
    			try {
	    			sleep 5
	    			sh 'curl "http://localhost:6800/hello"'
	    			sh 'curl "http://localhost:6800/shutdown"'
    			} catch(error) {
    				retry(2) {
		    			sleep 10
		    			sh 'curl "http://localhost:6800/hello"'
		    			sh 'curl "http://localhost:6800/shutdown"'
    				}
    			}
    		}
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
    		sh 'printenv'
    		rtMaven.deployer.deployArtifacts buildInfo
    		// buildInfo.append fullInfo 
		  server.publishBuildInfo buildInfo
		  
          def xrayConfig = [
            //Mandatory parameters
            // 'buildName'         : env.JOB_NAME,
            'buildName'         : buildInfo.name,
            'buildNumber'       : env.BUILD_NUMBER,

            //Optional
            'failBuild'        : false
          ]

          // Scan xray build
          def xrayResults = server.xrayScan xrayConfig
          
          // Print full report from xray
          // echo xrayResults as String
    }
    
    if (env.BRANCH_NAME == 'master') {
    		stage('Promotion') {
	    		echo 'promoting master branch!!!'
			def promotionConfig = [
			    // Mandatory parameters
			    'buildName'          : buildInfo.name,
			    'buildNumber'        : buildInfo.number,
			    'targetRepo'         : 'release-promotion',
			 
			    // Optional parameters
			    'comment'            : 'this is the promotion comment',
			    'sourceRepo'         : 'libs-release-local',
			    'status'             : 'Released',
			    'includeDependencies': true,
			    'copy'               : true,
			    // 'failFast' is true by default.
			    // Set it to false, if you don't want the promotion to abort upon receiving the first error.
			    'failFast'           : true
			]
			 
			// Promote build
			server.promote promotionConfig	    		
    		}
    }

}
