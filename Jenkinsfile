node {
    def server = Artifactory.server 'ART8080GCP'
	def rtMaven = Artifactory.newMavenBuild()
	def artDocker = Artifactory.docker server: server, host: "tcp://localhost:2375"
	def baseVersion = "3.1"
    	def buildVersion
    	def dockerTag = 'docker-local.artifactory.bruce/onboard/hello'
	def buildInfo = Artifactory.newBuildInfo()
	
	buildInfo.env.capture = true
	buildInfo.retention maxBuilds: 10
	
	if (env.BRANCH_NAME) {
		// override build name to avoid xray not recognizing :: in build name
		buildInfo.name = "${buildInfo.name.replace(':','-').replace(' ','')}"
	}
	
    stage('Checkout') {
    		// Get some code from a GitHub repository
    		if (env.BRANCH_NAME) {
			git url: 'https://github.com/brucefrog/JettyWorld', branch: env.BRANCH_NAME
		} else if (params.BRANCH_NAME) {
			git url: 'https://github.com/brucefrog/JettyWorld', branch: params.BRANCH_NAME
		} else {
			git url: 'https://github.com/brucefrog/JettyWorld'
		}
    }
    stage('Configure') {
		rtMaven.tool = 'Maven3.5.2'
		rtMaven.resolver server: server, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
	    rtMaven.deployer server: server, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
    		
    		// Transforming pom version number
    		if (env.BRANCH_NAME == 'master' || params.BRANCH_NAME == 'master') {
    			buildVersion = baseVersion + "." + env.BUILD_NUMBER
    		} else if (env.BRANCH_NAME == 'snapshot' || params.BRANCH_NAME == 'snapshot') {
    			buildVersion = baseVersion + "." + env.BUILD_NUMBER + "-SNAPSHOT"
    		} else {
    			buildVersion = baseVersion + "-UNKNOWN"
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
		buildImage = dockerTag + ":" + buildVersion
    		dir("docker") {
			def dockerImage = docker.build(buildImage)
			// dockerImage.tag("latest")
			def dockInfo = artDocker.push buildImage, 'docker-local', buildInfo 
			// dockerImage.push("latest")
			// artDocker.push dockerTag+":latest", 'docker'
			// buildInfo.append dockerInfo
		}
    }
    stage('Verify') {
        // sh 'docker rmi ' + dockerTag
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
			    'failFast'           : false
			]
			 
			// Promote build
			server.promote promotionConfig	    		
    		}
    		
    		stage('Distribution') {
			def jarDistCfg = [
			    // Mandatory parameters
			    'buildName'             : buildInfo.name,
			    'buildNumber'           : buildInfo.number,
			    'targetRepo'            : 'bintray', 
			        
			    // Optional parameters
			    'publish'               : true, // Default: true. If true, artifacts are published when deployed to Bintray.
			    'overrideExistingFiles' : false, // Default: false. If true, Artifactory overwrites builds already existing in the target path in Bintray.
			    'gpgPassphrase'         : 'bruce onboarding', // If specified, Artifactory will GPG sign the build deployed to Bintray and apply the specified passphrase.
			    'async'                 : false, // Default: false. If true, the build will be distributed asynchronously. Errors and warnings may be viewed in the Artifactory log.
			    "sourceRepos"           : ["release-promotion"], // An array of local repositories from which build artifacts should be collected.
			    'dryRun'                : false, // Default: false. If true, distribution is only simulated. No files are actually moved.
			]
			
			server.distribute jarDistCfg			

			def dkrDistCfg = [
			    // Mandatory parameters
			    'buildName'             : buildInfo.name,
			    'buildNumber'           : buildInfo.number,
			    'targetRepo'            : 'bintray', 
			        
			    // Optional parameters
			    'publish'               : true, // Default: true. If true, artifacts are published when deployed to Bintray.
			    'overrideExistingFiles' : true, // Default: false. If true, Artifactory overwrites builds already existing in the target path in Bintray.
			    'gpgPassphrase'         : 'bruce onboarding', // If specified, Artifactory will GPG sign the build deployed to Bintray and apply the specified passphrase.
			    'async'                 : false, // Default: false. If true, the build will be distributed asynchronously. Errors and warnings may be viewed in the Artifactory log.
			    "sourceRepos"           : ["docker-local"], // An array of local repositories from which build artifacts should be collected.
			    'dryRun'                : false, // Default: false. If true, distribution is only simulated. No files are actually moved.
			]
			
			server.distribute dkrDistCfg			
    		}
    }

}
