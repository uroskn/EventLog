#!/bin/bash
gcc -o eventprocessor.new processor.c $(pkg-config --cflags --libs sqlite3) -lrt -O2
