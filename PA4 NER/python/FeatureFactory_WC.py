import json, sys
import base64
import re

from Datum import Datum

class FeatureFactory:
    """
    Add any necessary initialization steps for your features here
    Using this constructor is optional. Depending on your
    features, you may not need to initialize anything.
    """
    def __init__(self):
        pass

    """
    Words is a list of the words in the entire corpus, previousLabel is the label
    for position-1 (or O if it's the start of a new sentence), and position
    is the word you are adding features for. PreviousLabel must be the
    only label that is visible to this method. 
    """

    def computeFeatures(self, words, previousLabel, position):
        features = []
        currentWord = words[position]  
        
        """ Baseline Features """
        features.append("word=" + currentWord)        
        #features.append("prevLabel=" + previousLabel)
        #features.append("word=" + currentWord + ", prevLabel=" + previousLabel)        
        
       
        if words[position]:
			features.append('TITLECASE')
        
        """ TODO: Add your features here """
        if currentWord[0].isupper() and currentWord[-1].islower():
			features.append('TITLE')
			
        #if currentWord[0].islower and len(currentWord) == 2 and currentWord[-1].isupper():
			#features.append('alTITLE')	
			
        #if currentWord[0].isalnum() and currentWord[0].islower():
			#features.append('ALPHANUM')
			
        if currentWord[0].isspace and currentWord[-1].isupper():
			features.append('NEWWORD')
			
       
		
        verb_pattern = r'.*ing|.*ed]' #ends in -ing or -ed
        pronoun_pattern = r'who|whose|she|he|I|a|an|as|at|by|his|me|or|thou|us'
        prep_pattern = r'aboard|about|above|across|after|against|along|amid|among|anti|around|as|at|before|behind|below|beneath|beside|besides|between|beyond|but|by|concerning|considering|despite|down|during|except|excepting|excluding|following|for|from|in|inside|into|like|minus|near|of|off|on|onto|opposite|outside|over|past|per|plus|regarding|round|save|since|than|through|to|toward|towards|under|underneath|unlike|until|up|upon|versus|via|with|within|without'
        suffixes = r'JR|SR|II|III|IV'
        if previousLabel != "O":
            if re.match(r'^[A-Z]', currentWord):
					features.append('INITCAP')
            #if re.match(r'^[A-Z]+$', currentWord):
					#features.append('ALLCAP')
            #if re.match(r'[a-zA-Z]+\w-+[a-zA-Z]*', currentWord):
					#features.append('HYPHEN')
           # if currentWord[0].isupper() and currentWord[-1].islower():
					#features.append('iTITLE')
            #if re.match(verb_pattern, words[position-1]):
					#features.append('VERB')
            #if re.match(pronoun_pattern, words[position-1]):
					#features.append('PRONOUN')
           # if re.match(suffixes, words[position-1]):
					#features.append('SUFFIX')
           
           
					
        #if previousLabel != "PERSON":
			#if re.match(r'.*[0-9].*', currentWord):
					#features.append('HASDIGIT')  
			#if re.match(prep_pattern, words[position-1]):
					#features.append('PREPS')
			
                    
        return features  
	
    """ Do not modify this method """
    def readData(self, filename):
        data = [] 
        
        for line in open(filename, 'r'):
            line_split = line.split()
            # remove empty lines
            if len(line_split) < 2:
                continue
            word = line_split[0]
            label = line_split[1]

            datum = Datum(word, label)
            data.append(datum)

        return data

    """ Do not modify this method """
    def readTestData(self, ch_aux):
        data = [] 
        
        for line in ch_aux.splitlines():
            line_split = line.split()
            # remove empty lines
            if len(line_split) < 2:
                continue
            word = line_split[0]
            label = line_split[1]

            datum = Datum(word, label)
            data.append(datum)

        return data


    """ Do not modify this method """
    def setFeaturesTrain(self, data):
        newData = []
        words = []

        for datum in data:
            words.append(datum.word)

        ## This is so that the feature factory code doesn't
        ## accidentally use the true label info
        previousLabel = "O"
        for i in range(0, len(data)):
            datum = data[i]

            newDatum = Datum(datum.word, datum.label)
            newDatum.features = self.computeFeatures(words, previousLabel, i)
            newDatum.previousLabel = previousLabel
            newData.append(newDatum)

            previousLabel = datum.label

        return newData

    """
    Compute the features for all possible previous labels
    for Viterbi algorithm. Do not modify this method
    """
    def setFeaturesTest(self, data):
        newData = []
        words = []
        labels = []
        labelIndex = {}

        for datum in data:
            words.append(datum.word)
            if not labelIndex.has_key(datum.label):
                labelIndex[datum.label] = len(labels)
                labels.append(datum.label)
        
        ## This is so that the feature factory code doesn't
        ## accidentally use the true label info
        for i in range(0, len(data)):
            datum = data[i]

            if i == 0:
                previousLabel = "O"
                datum.features = self.computeFeatures(words, previousLabel, i)

                newDatum = Datum(datum.word, datum.label)
                newDatum.features = self.computeFeatures(words, previousLabel, i)
                newDatum.previousLabel = previousLabel
                newData.append(newDatum)
            else:
                for previousLabel in labels:
                    datum.features = self.computeFeatures(words, previousLabel, i)

                    newDatum = Datum(datum.word, datum.label)
                    newDatum.features = self.computeFeatures(words, previousLabel, i)
                    newDatum.previousLabel = previousLabel
                    newData.append(newDatum)

        return newData

    """
    write words, labels, and features into a json file
    Do not modify this method
    """
    def writeData(self, data, filename):
        outFile = open(filename + '.json', 'w')
        for i in range(0, len(data)):
            datum = data[i]
            jsonObj = {}
            jsonObj['_label'] = datum.label
            jsonObj['_word']= base64.b64encode(datum.word)
            jsonObj['_prevLabel'] = datum.previousLabel

            featureObj = {}
            features = datum.features
            for j in range(0, len(features)):
                feature = features[j]
                featureObj['_'+feature] = feature
            jsonObj['_features'] = featureObj
            
            outFile.write(json.dumps(jsonObj) + '\n')
            
        outFile.close()

