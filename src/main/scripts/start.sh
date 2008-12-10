#!/bin/bash
#
# Startup script for Jetty
#

# resolve links - $0 may be a softlink
THIS="$0"
while [ -h "$THIS" ]; do
  ls=`ls -ld "$THIS"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    THIS="$link"
  else
    THIS=`dirname "$THIS"`/"$link"
  fi
done

THIS_DIR=`dirname "$THIS"`
JETTY_HOME=`cd "$THIS_DIR" ; pwd`

START_JAR=$JETTY_HOME/start.jar
LOG="$JETTY_HOME/console.log"
LOCK="$JETTY_HOME/.jetty.lock"
export JETTY_HOME


RETVAL=0

pid_of_jetty() {
    ps auxwww | grep java | grep start.jar | grep jetty | grep -v grep | awk '{print $2}'
}

start() {
    [ -e "$LOG" ] && cnt=`wc -l "$LOG" | awk '{ print $1 }'` || cnt=1

    echo -n $"Starting jetty: $JETTY_HOME "

    cd $JETTY_HOME
    nohup $JAVA_HOME/bin/java -Djetty.port=8088 -Xmx512m -jar "$START_JAR" >> "$LOG" 2>&1 &

    while { pid_of_jetty > /dev/null ; } &&
           { tail +$cnt "$LOG" | grep -q 'Winstone Servlet Engine .* running' ; } ; do
        sleep 1
    done

    pid_of_jetty > /dev/null
    RETVAL=$?
    [ $RETVAL = 0 ] && echo "[started ($STRING)]" || echo "[failed ($STRING)]"
    echo

    [ $RETVAL = 0 ] && touch "$LOCK"
}

stop() {
    echo -n "Stopping jetty: "

    pid=`pid_of_jetty`
    
    [ -n "$pid" ] && kill $pid
    RETVAL=$?
    cnt=10
    while [ $RETVAL = 0 -a $cnt -gt 0 ] &&
          { pid_of_jetty > /dev/null ; } ; do
        sleep 1
        ((cnt--))
    done

    [ $RETVAL = 0 ] && rm -f "$LOCK"
    [ $RETVAL = 0 ] && echo "[stopped ($STRING)]" || echo "[failed ($STRING)]"
    echo
}

status() {
    pid=`pid_of_jetty`
    if [ -n "$pid" ]; then
        echo "jetty (pid $pid) is running..."
        return 0
    fi
    if [ -f "$LOCK" ]; then
        echo $"${base} dead but subsys locked"
        return 2
    fi
    echo "jetty is stopped"
    return 3
}

# See how we were called.
case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  status)
    status
    ;;
  restart)
    stop
    start
    ;;
  *)
    echo $"Usage: $0 {start|stop|restart|status}"
    exit 1
esac

exit $RETVAL
