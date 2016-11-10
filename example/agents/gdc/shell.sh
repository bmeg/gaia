#!/bin/bash

docker build -t bmeg/gdc-scan .

docker run -ti --rm -u `id -u` -e HOME=$HOME -v $HOME:$HOME -w `pwd` bmeg/gdc-scan /bin/bash