#!/bin/bash
java -Xmx4g -jar AnagramsServer.jar &
./bin/restart.sh &
wait -n
exit $?