#!/bin/bash

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/comptel/elink/catcry/PardisRequestBuilder/lib/EL_LIB_BLT_LOOKUP/Linux/
rm -rf bin/ control/ discarded/ out/ rejected/ reprocess/ storage/ temp/ in/ audit/ log/
mkrte.pl cfg/cfg.xml
#cp ./arx/decoded-chunker-input-02_1724075025x001_1_9  ./in/
cp ./arx/new_decoded ./in/
java_node -c control/config 

