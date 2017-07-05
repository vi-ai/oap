
CP="$APPHOME/conf"
for f in $APPHOME/lib/*.jar
do
    CP=$CP:$f
done

VM_OPTS=""
if [ -f $APPHOME/conf/vm.options ]
then
    for opt in `cat $APPHOME/conf/vm.options`
    do
        opt=`eval echo $opt`
        VM_OPTS="$VM_OPTS $opt"
    done
fi

[ -f $APPHOME/bin/env.sh ] && . $APPHOME/bin/env.sh

if [ 0 -eq $UID ]
then
    PIDFILE=/var/run/$APPNAME.pid
else
    PIDFILE=/var/tmp/$APPNAME.pid
fi

oap_status() {
    if [ ! -f $PIDFILE ]
    then
        echo $"$APPNAME is stopped"
        return 3
    fi
    pid=`cat $PIDFILE`
    if [ ! -z `ps --pid $pid -opid=` ]
    then
        echo $"$APPNAME is running..."
        return 0
    else
        echo $"$APPNAME dead but pid file exists"
        return 1
    fi
}

oap_status_q() {
    oap_status > /dev/null 2>&1
}


oap_start() {
    $SUEXEC java $VM_OPTS -cp $CP oap.application.Boot --start --config=$APPHOME/conf/application.conf > /var/log/$APPNAME/console.log 2>&1 &
    retval=$?
    pid=$!
    [ $retval -eq 0 ] && echo $pid > $PIDFILE
    return $retval
}

oap_run() {
    java $VM_OPTS -cp $CP oap.application.Boot --start --config=$APPHOME/conf/application.conf
    retval=$?
    return $retval
}


oap_waitforpid() {
    while test -d "/proc/$1"; do
        sleep 0.2
    done
    return 0
}

oap_stop() {
    pid=`cat $PIDFILE`
    [ ! -z `ps --pid $pid -opid=` ] && kill $pid && oap_waitforpid $pid
    retval=$?
    [ $retval -eq 0 ] && rm $PIDFILE
    return $retval
}

