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
        """ TODO: Add your features here """
        prevWord = words[position - 1] if position > 0 else "."
        prevWord2 = words[position - 2] if position > 1 else "."       
        nextWord = words[position + 1] if position + 1 < len(words) else "."
        nextWord2 = words[position + 2] if position + 2 < len(words) else "."   
       
        features.append(currentWord)         
        features.append("prevWord=" + currentWord)
        features.append("nextWord=" + currentWord)
        features.append("prevWord2=" + currentWord)        
        features.append("word=" + currentWord)
        features.append("prevWord=" + prevWord)        
        features.append("prevWord2=" + prevWord2)
        features.append("nextWord=" + nextWord)
        features.append("nextWord2=" + nextWord2)
      
        features.append("initial=0" + currentWord)
        
        
        if currentWord[0].isupper():
            features.append("FirstLastName=" + currentWord + " " + nextWord)
            
        if currentWord[0].isupper():
            features.append("abbreviated=" + currentWord[-1:] + ".")
       
        if currentWord.isspace and currentWord[-1].isupper():
	    features.append("spacecase=")
	    
        if currentWord[0].isupper():
            features.append("suffixUpper=" + currentWord[-3:])
            
        if currentWord[0].islower:
	    features.append("suffixLower=" + currentWord[-3:])
	    
        if currentWord:
	    features.append("presentcase=")

        if currentWord.istitle():
            features.append("CAPS=")
         	
        if currentWord[0].isupper() and currentWord[-1].islower():
	    features.append("UPPERCASE=")
	    
        if previousLabel != "O":
            if re.findall(r'^[A-Z]', currentWord):
	        features.append("INITCAP=")	
			
       	    
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

