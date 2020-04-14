#!/bin/bash

find 1m_faces_00_01_02_03/ -name '*.jpg' -exec mogrify mogrify -resize 150x150 -quality 30%  -path small *.jpg {} +

