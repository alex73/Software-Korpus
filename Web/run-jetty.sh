#!/bin/sh
set -x

ant fastwar || exit 1
mv build/korpus.war ~/jetty/webapps/
