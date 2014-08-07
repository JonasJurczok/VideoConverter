#Welcome to the VideoConverter project.#

##Goal of this tool##

This tool is intended to give some quick command line based access to ffmpeg. 
While ffmpeg can do a lot of stuff itself it is not possible to run a batch for multiple files at once.
Of course you can write a script, but especially if you want to use different output 
directories/naming schemes and maybe different target resolutions such scripts can become quite complicated.

##Supported configurations##

###ffmpeg###
Directory of ffmpeg with trailing / and without the name of the binary.

###dryRun###
Do a dry run? Just write logs but don't do anything.

###inputDir###
The directory where all the video files are taken from.

###outputDir###
The directory where the videos are rendered to. Output videos will be rendered into a subfolder named with the project name derived from the original file with the projectDelimiter

####Example:####
	'input/Project - video.avi' leads to 'output/Project/video.avi' given a project delimiter of '-'
	
###intermediateDir###
If specified this directory is used to store intermediate files (e.g. files with fading effects before adding the intro).

###projectDelimiter###
Delimiter to find the project name with.

###outputSuffix###

###originalSuffix###
Suffix for the original file if it is not deleted. It will be moved to the project directory then.

###deleteOriginalFile###
Should the original file be deleted after processing?

###targetFps###
FPS of the output file.

###targetResolution###
Resolution of the output file. **Example:** targetResolution=1920x1200

###introFile###
A video file that should be rendered into the beginning of the output. Like an intro for a video should.

###fadeInDuration###

###fadeOutDuration###


##Scenarios##


 
 