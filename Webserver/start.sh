#!/bin/bash
./Server/bin/AnagramsServer &
./Client/bin/restart.sh &
wait -n
exit $?