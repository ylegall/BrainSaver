#!/bin/sh

READLINK=`which readlink`

SCRIPT_LOCATION=$0
if [ -x "$READLINK" ]; then
  while [ -L "$SCRIPT_LOCATION" ]; do
    SCRIPT_LOCATION=`"$READLINK" -e "$SCRIPT_LOCATION"`
  done
fi

BIN_HOME=`dirname "$SCRIPT_LOCATION"`
if [ "$BIN_HOME" = "." ]; then
  ROOT_HOME=".."
else
  ROOT_HOME=`dirname "$BIN_HOME"`
fi

CLASSPATH=
for i in `ls ${ROOT_HOME}/target/*.jar`
do
  CLASSPATH=${CLASSPATH}:${i}
done

java -cp ${CLASSPATH} org.ygl.InterpreterKt "$@"