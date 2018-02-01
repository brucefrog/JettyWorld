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
		} else if (param.BRANCH_NAME) {
			git url: 'https://github.com/brucefrog/JettyWorld', branch: param.BRANCH_NAME
		} else {
			git url: 'https://github.com/brucefrog/JettyWorld'
		}
    }
    stage('Java Build') {
		// Setup Artifactory resolution
		rtMaven.deployer.deployArtifacts = false
        rtMaven.deployer.addProperty("MyProp","Hello")
    		if (env.BRANCH_NAME) {
    			echo "attempting to transform version number"
			def descriptor = Artifactory.mavenDescriptor()
			descriptor.version = '1.0.0'
			descriptor.pomFile = 'pom.xml'
    			descriptor.setVersion "bruce.jfrog:JettyParent", "1.0." + env.BUILD_NUMBER
    			descriptor.transform()
    		}
		buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean package -DBUILD=' + env.BUILD_NUMBER
    		if (env.BRANCH_NAME) {
    			buildInfo.name = 'JettyWorld-' + env.BRANCH_NAME
    		}
		buildInfo.env.capture = true
		buildInfo.retention maxBuilds: 10
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
	stage('Deploy') {
		rtMaven.deployer.deployArtifacts = false
		def buildInfo5 = rtMaven.run pom: 'pom.xml', goals: 'install -DBUILD=' + env.BUILD_NUMBER
		buildInfo.append buildInfo5
		
		// rtMaven.deployer.deployArtifacts buildInfo 
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
    		sh 'printenv'
    		rtMaven.deployer.deployArtifacts buildInfo 
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
			    'targetRepo'         : 'libs-release-local',
			 
			    // Optional parameters
			    'comment'            : 'this is the promotion comment',
			    'sourceRepo'         : 'libs-release-local',
			    'status'             : 'Released',
			    'includeDependencies': true,
			    'copy'               : false,
			    // 'failFast' is true by default.
			    // Set it to false, if you don't want the promotion to abort upon receiving the first error.
			    'failFast'           : true
			]
			 
			// Promote build
			server.promote promotionConfig	    		
    		}
    }

}
