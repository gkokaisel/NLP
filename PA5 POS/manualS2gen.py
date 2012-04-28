#!/usr/bin/env python
# -*- coding: utf-8 -*-
'''
NLP: this is my python script to generate an appropriate S2.gr: 
Change the words in s2 table to generate your own grammar.
'''

outFile = open("S2.gr", 'w')

s2 = ['Det','Noun','Prep','Proper','VerbT', 'Misc', 'CC', 'CD', 'IN', 'Modal', 'JJ', 'NNP', 'PRP', 'PRP$', 'RB', 'Do', 'Does', 'To', 'VerbBF', 'VBD', 'VBG', 'VBG', 'VBN', 'VBZ', 'VB', 'WP', 'WRB', 'WP$' ]

outFile.write('1\t' + 'S2' + '\n')
for word in s2:
    outFile.write('1\t' + 'S2' + '\t' + '_'+str(word) + '\n') 
for word in s2:
    outFile.write('1\t' + '_'+str(word) + '\t' +str(word) + '\n') 
    for s in s2:
        outFile.write('1\t' + '_'+str(word) + '\t' +str(word) + ' ' + '_'+str(s) + '\n')   
outFile.close()

if __name__ == "__main__":
    print "gosh!"
