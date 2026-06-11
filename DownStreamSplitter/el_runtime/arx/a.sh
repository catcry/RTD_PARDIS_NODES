#!/bin/bash

#!/bin/bash

flag=0
while read -r line; do
    if [ "$line" == "RECORD" ]; then
        if [ $flag -eq 1 ]; then
            break
        fi
        flag=1
    else
        if [ $flag -eq 1 ]; then
            echo "$line"
        fi
    fi
done < $1


