#!/bin/bash
. $(dirname $(realpath $0))/../shell/loader.sh

writepid eventlog

CPATH=$(dirname $0);
cd $CPATH

cgcreate -g cpu:eventlog
echo 1 >/cgroup/eventlog/cpu.shares
echo 0 >/cgroup/eventlog/cpuset.cpus
echo 0 >/cgroup/eventlog/cpuset.mems

( cgclassify -g cpu:eventlog $$ ; renice -n 1 -p $$; while [ true ]; do ./eventprocessor /srv/workdir/events.queue.sqlite <../../../craftbukkit/eventlog; done ) &

ionice -c 3 -p $$
while [ true ]; do
  nice -n 1 cgexec -g cpu:eventlog php -f process.php 2>&1 >data.log
  sleep 3600 ;
done;
