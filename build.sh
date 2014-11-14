NOW="$(date +'%Y-%m-%d')"
SHARE_DIR=/Users/nribeka/Dropbox/Android/dev/
BUILD_DIR=/Users/nribeka/Documents/Work/Build

ANDROID_HOME=/Users/nribeka/Documents/Work/Android

MUZIMA_REPO=https://github.com/muzima/muzima-android.git
MUZIMA_PROJECT=muzima-android
MUZIMA_APPS=muzima-android-1.0-SNAPSHOT.apk

export ANDROID_HOME=$ANDROID_HOME

cd $SHARE_DIR
mkdir $NOW

cd $BUILD_DIR
rm -rf $MUZIMA_PROJECT

git clone $MUZIMA_REPO $MUZIMA_PROJECT
cd $MUZIMA_PROJECT

git checkout master
git pull
mvn clean install -Dandroid.sdk.path=$ANDROID_HOME
cp $BUILD_DIR/$MUZIMA_PROJECT/target/$MUZIMA_APPS
$SHARE_DIR/$NOW/muzima-android-teleconsultation.apk

git checkout 1.0
git pull
mvn clean install -Dandroid.sdk.path=$ANDROID_HOME
cp $BUILD_DIR/$MUZIMA_PROJECT/target/$MUZIMA_APPS
$SHARE_DIR/$NOW/muzima-android.apk

