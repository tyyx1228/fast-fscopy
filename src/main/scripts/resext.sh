BIN_DIR=$(cd $(dirname "${BASH_SOURCE-$0}")>/dev/null; pwd)
APP_HOME=$(cd $BIN_DIR/.. >/dev/null; pwd)

LIB_DIR=$APP_HOME/lib
LIB_JARS=`ls $LIB_DIR | awk '{print "'$LIB_DIR'/"$0}' | tr "\n" ":"`

java -cp $LIB_JARS com.bigdata.FileSuffixRename $@