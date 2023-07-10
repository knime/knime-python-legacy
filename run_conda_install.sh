#!/bin/bash
$CONDA_PATH info
$CONDA_PATH env create -f $WORKSPACE/$YML_PATH/${ENV_FILE}.yml -p $WORKSPACE/$ENV_FILE --quiet --json --solver=classic
$CONDA_PATH list -p $WORKSPACE/$ENV_FILE 
