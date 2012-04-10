#-------------------------------------------------------------------------------
# Name: NLP PA1 SpamLord
# Purpose:  regular expressions to extract phone numbers and email addressess
#
# Author:      gkokaisel
#
# Created:     15/03/2012
# Copyright:   (c) gkokaisel 2012
# Licence:     <your licence>
#-------------------------------------------------------------------------------
#!/usr/bin/env python

import sys
import os
import re
import pprint

suffixes = parse_suffix_list("suffix_list.txt")


def is_domain(d):
    for suffix in suffixes:
        if d.endswith(suffix):
            # Get the base domain name without suffix
            base_name = d[0:-(suffix.length + 1)]
            # If it contains '.', it's a subdomain. 
            if not base_name.contains('.'):
                return true
    # If we get here, no matches were found
    return false
