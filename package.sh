#!/bin/sh

PACKAGE_DIR="./output_pkgs/"
PROJECT_NAME="UglyPic"
TEMP_MANIFEST="AndroidManifest.xml_tmp"

#clear intermediate files
function before_building()
{
	echo "clear intermediate files   >>>>>>>>>>>"
	ant clean
	rm -rf bin
	rm -rf $PACKAGE_DIR
	mkdir $PACKAGE_DIR
	cp AndroidManifest.xml $TEMP_MANIFEST
	echo "env initialized, start to build   >>>>>>>>>>>"
}

function after_building()
{
    TIME=`date +%Y%m%d%H%M`
 	cp -R ./bin/proguard $PACKAGE_DIR/proguard-$TIME
	cp $TEMP_MANIFEST AndroidManifest.xml
	rm $TEMP_MANIFEST
	echo "****************************************************************"
	echo "*            Building Completes !                               *"
	echo "****************************************************************"
}

#build in debug mode 
function build_debug() 
{
	echo "build debug package   >>>>>>>>>>"
	ant debug;
	cp ./bin/$PROJECT_NAME-debug.apk $PACKAGE_DIR
}

#build in release mode only
function build_release()
{
	echo "build release package   >>>>>>>>>>"
	ant release;
	cp ./bin/$PROJECT_NAME-release.apk $PACKAGE_DIR
}

#build for channel 
function build_channel()
{
	echo "build for channel: ${1:DEFAULT}   >>>>>>>>>>"
	ant clean
	#替换CHANNEL名字
	#cp $TEMP_MANIFEST AndroidManifest.xml
	sed "s/android:value=\"DEFAULT\"/android:value=\"${1:DEFAULT}\"/" $TEMP_MANIFEST > AndroidManifest.xml
	ant release
	cp ./bin/$PROJECT_NAME-release.apk $PACKAGE_DIR/$PROJECT_NAME_${1:DEFAULT}.apk
	echo "compelete building for channel: ${1:DEFAULT}   >>>>>>>>>>"
}

function print_usage()
{
	echo "[Usage]: sh package.sh [parameter]"
	echo "[parameter] must be:"
	echo "	debug:    for debug apk"
	echo "	release:  for release apk"
	echo "	channel:  for release apk with channel specified in \"channels.config\""
	echo "";
}


# Main Entrance
if [ $# -ne 1 ]
then	
		print_usage	
else
		case "$1" in
	    debug)
			before_building
			build_debug
			after_building
			;;
		release)
			before_building
			build_release
			after_building
			;;
		channel)
			before_building

			for ch in `cat channels.config`
			do
				build_channel $ch
			done

			after_building
		    ;;
		*)
			print_usage
		esac
fi
